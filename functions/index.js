const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

const db = admin.firestore();

exports.atualizarAgendamentos = functions
  .region("southamerica-east1")
  .https.onRequest(async (req, res) => {
  try {
    const { DateTime } = require("luxon");

    const now = DateTime.now().setZone("America/Sao_Paulo");
    const dataAtual = now.toFormat("yyyy-MM-dd");
    const horaAtual = now.toFormat("HH:mm");

    const cincoMinDepois = now.plus({ minutes: 5 }).toFormat("HH:mm");

    const snapshot = await db.collectionGroup("reservations")
      .where("date", "==", dataAtual)
      .where("status", "in", ["confirmado", "em_andamento"])
      .get();

    let atualizados = 0;

    for (const doc of snapshot.docs) {
      const ag = doc.data();
      const ref = doc.ref;

      if (!ag.startTime || !ag.endTime || !ag.userId) continue;

      let token = null;
      try {
        const userSnap = await db.collection("users").doc(ag.userId).get();
        if (userSnap.exists && userSnap.data().fcmToken) {
          token = userSnap.data().fcmToken;
        }
      } catch (e) {
        console.warn("Erro ao buscar fcmToken do usuário:", ag.userId, e.message);
      }

      // Notificação: começa em 5 minutos
      if (ag.startTime === cincoMinDepois && ag.status === "confirmado" && token) {
        await admin.messaging().send({
          notification: {
            title: "Sua reserva começa em 5 minutos!",
            body: "Prepare-se para utilizar a reserva. Chegue no horário!",
          },
          token: token,
        }).catch(e => console.warn("Erro ao enviar notificação pré-início:", e.message));
      }

      // Notificação: termina em 5 minutos
      if (ag.endTime === cincoMinDepois && ag.status === "em_andamento" && token) {
        await admin.messaging().send({
          notification: {
            title: "Sua reserva termina em 5 minutos!",
            body: "Se adiante para não atrapalhar o próximo uso. Libere o uso do recurso no horário.",
          },
          token: token,
        }).catch(e => console.warn("Erro ao enviar notificação pré-fim:", e.message));
      }

      // Início da reserva
      if (ag.startTime <= horaAtual && ag.endTime > horaAtual && ag.status !== "em_andamento") {
        await ref.update({ status: "em_andamento" });
        atualizados++;

        if (token) {
          await admin.messaging().send({
            notification: {
              title: "Sua reserva começou!",
              body: "Sua reserva já está disponível. Vá até o local para usá-la.",
            },
            token: token,
          }).catch(e => console.warn("Erro ao enviar notificação início:", e.message));
        }
      }

      // Fim da reserva
      else if (ag.endTime <= horaAtual && ag.status !== "finalizado") {
        await ref.update({ status: "finalizado" });
        atualizados++;

        if (token) {
          await admin.messaging().send({
            notification: {
              title: "Sua reserva terminou!",
              body: "O tempo da sua reserva foi encerrado. Obrigado por usar o LavaFácil!",
            },
            token: token,
          }).catch(e => console.warn("Erro ao enviar notificação fim:", e.message));
        }
      }
    }

    res.status(200).send(`Atualizados ${atualizados} reservations.`);
  } catch (err) {
    console.error("Erro ao atualizar agendamentos:", err);
    res.status(500).send("Erro interno: " + err.message);
  }
});

exports.notifyNewChatMessage = functions
  .region("southamerica-east1")
  .firestore
  .document("buildings/{buildingId}/chat/{messageId}")
  .onCreate(async (snap, context) => {
    const { buildingId, messageId } = context.params;
    const msg = snap.data();

    // Campos do documento do chat
    const text = (msg?.message || "").toString();
    const senderId = (msg?.senderId || "").toString();
    const senderName = (msg?.senderName || "Novo comentário").toString();

    try {
      // 0) Nome do prédio para admins
      let buildingName = buildingId;
      try {
        const bDoc = await db.collection("buildings").doc(buildingId).get();
        if (bDoc.exists) {
          buildingName = bDoc.get("name") || bDoc.get("title") || buildingId;
        }
      } catch (_) {}

      // 1) USERS do prédio atual (recebem, menos o remetente)
      const usersSnap = await db.collection("users")
        .where("type", "==", "user")
        .where("buildingId", "==", buildingId)
        .get();

      // 2) ADMINS que administram esse prédio (recebem, menos o remetente)
      const adminsSnap = await db.collection("users")
        .where("type", "==", "admin")
        .where("buildings", "array-contains", buildingId)
        .get();

      // 3) Coletar tokens (mantendo doc junto para filtrar remetente)
      const collectTargets = (qs) => {
        const arr = [];
        qs.forEach(doc => {
          const t = doc.get("fcmToken");
          if (typeof t === "string" && t.trim()) {
            arr.push({ token: t.trim(), doc });
          }
        });
        return arr;
      };
      let userTargets  = collectTargets(usersSnap);
      let adminTargets = collectTargets(adminsSnap);

      // 4) Excluir SEMPRE o remetente (compara doc.id e campo uid)
      const filterOutSender = (targets) => targets.filter(({ doc }) => {
        const docUid = doc.get("uid"); // agora você já preencheu para admin também
        const isSender = (doc.id === senderId) || (docUid && docUid === senderId);
        return !isSender;
      });
      userTargets  = filterOutSender(userTargets);
      adminTargets = filterOutSender(adminTargets);

      // 5) Deduplicar tokens e transformar em string[]
      const toTokenArray = (targets) => Array.from(new Set(targets.map(t => t.token)));
      const userTokens  = toTokenArray(userTargets);
      const adminTokens = toTokenArray(adminTargets);

      if (userTokens.length === 0 && adminTokens.length === 0) {
        console.log("[notifyNewChatMessage] Sem destinatários após filtros.");
        return null;
      }

      // 6) Payloads
      const preview = text.length > 120 ? `${text.substring(0, 117)}...` : text;

      const notificationUser = {
        title: `[${buildingName}] ${senderName} no chat`,
        body: preview,
      };
      const dataUser = {
        type: "chat",
        buildingId,
        buildingName,
        messageId,
        senderId,
        senderName,
      };

      // 7) Envio em chunks (<= 500)
      const sendChunks = async (tokens, notification, data) => {
        const chunkSize = 500;
        let ok = 0, fail = 0;
        for (let i = 0; i < tokens.length; i += chunkSize) {
          const res = await admin.messaging().sendEachForMulticast({
            tokens: tokens.slice(i, i + chunkSize),
            notification,
            data,
            android: {
              priority: "high",
              notification: { channelId: "default_channel", sound: "default" },
            },
            apns: { payload: { aps: { sound: "default" } } },
          });
          ok  += res.successCount;
          fail += res.failureCount;
        }
        return { ok, fail };
      };

      let okTotal = 0, failTotal = 0;

      const r = await sendChunks(userTokens, notificationUser, dataUser);
      okTotal += r.ok; failTotal += r.fail;



      console.log(`[notifyNewChatMessage] OK - ${okTotal} sucesso, ${failTotal} falhas`);
      return null;

    } catch (e) {
      console.error("[notifyNewChatMessage] Erro:", e);
      return null;
    }
  });

exports.attachTokenToUser = functions
  .region("southamerica-east1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Login necessário.");
    }
    const uid = context.auth.uid;                  // <- usa o uid autenticado
    const token = (data?.token || "").trim();
    if (!token) {
      throw new functions.https.HttpsError("invalid-argument", "Token ausente.");
    }

    const db = admin.firestore();

    // localizar o doc do usuário pelo campo 'uid'
    const targetSnap = await db.collection("users").where("uid", "==", uid).limit(1).get();
    if (targetSnap.empty) {
      throw new functions.https.HttpsError("not-found", "users doc com este uid não encontrado.");
    }
    const targetRef = targetSnap.docs[0].ref;

    // remover este token de quaisquer OUTROS docs
    const dupSnap = await db.collection("users").where("fcmToken", "==", token).get();

    const batch = db.batch();
    dupSnap.forEach(d => {
      if (d.ref.path !== targetRef.path) {
        batch.update(d.ref, { fcmToken: admin.firestore.FieldValue.delete() });
      }
    });

    // anexar o token ao usuário atual
    batch.set(targetRef, { fcmToken: token }, { merge: true });
    await batch.commit();

    return { ok: true, attachedTo: targetRef.id, removedFrom: Math.max(dupSnap.size - 1, 0) };
  });

