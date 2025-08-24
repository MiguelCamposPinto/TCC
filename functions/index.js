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
