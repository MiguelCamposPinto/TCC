const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

const db = admin.firestore();

exports.atualizarAgendamentos = functions.https.onRequest(async (req, res) => {
  try {
    const { DateTime } = require("luxon");

    const now = DateTime.now().setZone("America/Sao_Paulo");
    const dataAtual = now.toFormat("yyyy-MM-dd"); // Ex: 2025-06-07
    const horaAtual = now.toFormat("HH:mm");      // Ex: 22:41

    const snapshot = await db.collectionGroup("agendamentos")
      .where("data", "==", dataAtual)
      .where("status", "in", ["confirmado", "em_andamento"])
      .get();

    let atualizados = 0;

    for (const doc of snapshot.docs) {
      const ag = doc.data();
      const ref = doc.ref;

      if (!ag.horaInicio || !ag.horaFim || !ag.userId) continue;

      let token = null;
      try {
        const userSnap = await db.collection("users").doc(ag.userId).get();
        if (userSnap.exists && userSnap.data().fcmToken) {
          token = userSnap.data().fcmToken;
        }
      } catch (e) {
        console.warn("Erro ao buscar fcmToken do usuário:", ag.userId, e.message);
      }

      // Início da reserva
      if (ag.horaInicio <= horaAtual && ag.horaFim > horaAtual && ag.status !== "em_andamento") {
        await ref.update({ status: "em_andamento" });
        atualizados++;

        if (token) {
          await admin.messaging().send({
            notification: {
              title: "Sua reserva começou!",
              body: "Sua máquina já está disponível. Vá até o local para usá-la.",
            },
            token: token,
          }).catch(e => console.warn("Erro ao enviar notificação início:", e.message));
        }

      // Fim da reserva
      } else if (ag.horaFim <= horaAtual && ag.status !== "finalizado") {
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

    res.status(200).send(`Atualizados ${atualizados} agendamentos.`);
  } catch (err) {
    console.error("Erro ao atualizar agendamentos:", err);
    res.status(500).send("Erro interno: " + err.message);
  }
});
