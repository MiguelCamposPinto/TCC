const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();
const { Client } = require("pg");

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


 const { Pool } = require('pg');
 // --------- PG CONFIG ---------
 const PG_CONFIG = {
   host: 'ep-lingering-darkness-ac74wunk-pooler.sa-east-1.aws.neon.tech',
   port: 5432,
   database: 'neondb',
   user: 'neondb_owner',
   password: 'npg_z5RxvhwW3CTo',
   ssl: require,
 };

 const pool = new Pool(PG_CONFIG);

 // Helpers
 const toTime = (v) => (typeof v === 'string' && /^\d{2}:\d{2}$/.test(v) ? v : null);
 const toDateOnly = (v) => (typeof v === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(v) ? v : null);

 const UPSERT_SQL = `
   INSERT INTO reservas (
     id, building_id, date, duration_min, start_time, end_time, status,
     firestore_path, machine_id, machine_name, space_id, space_name, space_type,
     user_id, user_name, updated_at
   )
   VALUES (
     $1, $2, $3, $4, $5, $6, $7,
     $8, $9, $10, $11, $12, $13,
     $14, $15, NOW()
   )
   ON CONFLICT (id) DO UPDATE SET
     building_id    = EXCLUDED.building_id,
     date           = EXCLUDED.date,
     duration_min   = EXCLUDED.duration_min,
     start_time     = EXCLUDED.start_time,
     end_time       = EXCLUDED.end_time,
     status         = EXCLUDED.status,
     firestore_path = EXCLUDED.firestore_path,
     machine_id     = EXCLUDED.machine_id,
     machine_name   = EXCLUDED.machine_name,
     space_id       = EXCLUDED.space_id,
     space_name     = EXCLUDED.space_name,
     space_type     = EXCLUDED.space_type,
     user_id        = EXCLUDED.user_id,
     user_name      = EXCLUDED.user_name,
     updated_at     = NOW();
 `;

 const DELETE_SQL = `DELETE FROM reservas WHERE id = $1;`;

 exports.syncAgendamento = functions
   .region("southamerica-east1")
   .firestore
   .document("buildings/{buildingId}/spaces/{spaceId}/{resourceType}/{resourceId}/reservations/{reservaId}")
   .onWrite(async (change, context) => {
     const {
       buildingId,
       spaceId,
       resourceType,
       resourceId,
       reservaId
     } = context.params;

     // Garante que estamos em uma coleção válida
     const allowed = new Set(['machines', 'quadras', 'saloes']);
     if (!allowed.has(resourceType)) {
       console.warn('Ignorando resourceType não suportado:', resourceType);
       return null;
     }

     const client = await pool.connect();
     try {
       // DELETE
       if (!change.after.exists) {
         await client.query(DELETE_SQL, [reservaId]);
         console.log('Removido do Postgres:', reservaId);
         return null;
       }

       // CREATE/UPDATE
       const data = change.after.data() || {};
       const id = reservaId;

       const date = toDateOnly(data.date);                   // 'YYYY-MM-DD'
       const durationMin = data.durationMin ?? null;         // int
       const startTime = toTime(data.startTime);             // 'HH:MM'
       const endTime = toTime(data.endTime);                 // 'HH:MM'
       const status = data.status || 'indefinido';

       const firestorePath = data.firestorePath ?? null;

       // nomes (se não vierem no doc da reserva, tenta pegar do recurso/espaço)
       // Para pegar os nomes do recurso/espaço, faríamos reads extras no Firestore.
       // Para não custar reads, usamos o que veio no documento:
       const machineName = data.machineName ?? null;
       const spaceName   = data.spaceName ?? null;
       const spaceType   = data.spaceType ?? resourceType;   // fallback: nome da coleção

       const userId   = data.userId || '';
       const userName = data.userName ?? null;

       // Valida campos NOT NULL
       if (!date || !startTime || !endTime || !userId) {
         console.warn('Reserva com campos obrigatórios ausentes. id=', id, { date, startTime, endTime, userId });
         // Opcional: abortar para não quebrar a linha
         // return null;
       }

       await client.query(UPSERT_SQL, [
         id, buildingId, date, durationMin, startTime, endTime, status,
         firestorePath, resourceId, machineName, spaceId, spaceName, spaceType,
         userId, userName
       ]);

       console.log('Upsert OK:', id, '(', resourceType, ')');
       return null;
     } catch (err) {
       console.error('Erro ao sincronizar reserva:', err);
       throw err;
     } finally {
       client.release();
     }
   });

