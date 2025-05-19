const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();

exports.atualizarAgendamentos = functions.https.onRequest(async (req, res) => {
  try {
    const { DateTime } = require("luxon");

    const now = DateTime.now().setZone("America/Sao_Paulo");

    const dataAtual = now.toFormat("yyyy-MM-dd"); // Ex: 2025-05-18
    const horaAtual = now.toFormat("HH:mm");      // Ex: 14:30


    const snapshot = await db.collectionGroup("agendamentos")
      .where("data", "==", dataAtual)
      .where("status", "in", ["confirmado", "em_andamento"])
      .get();

    let atualizados = 0;

    for (const doc of snapshot.docs) {
      const ag = doc.data();
      const ref = doc.ref;

      if (!ag.horaInicio || !ag.horaFim) continue;

      if (ag.horaInicio <= horaAtual && ag.horaFim > horaAtual && ag.status !== "em_andamento") {
        await ref.update({ status: "em_andamento" });
        atualizados++;
      } else if (ag.horaFim <= horaAtual && ag.status !== "finalizado") {
        await ref.update({ status: "finalizado" });
        atualizados++;
      }
    }

    res.status(200).send(`Atualizados ${atualizados} agendamentos.`);
  } catch (err) {
    console.error("Erro ao atualizar agendamentos:", err);
    res.status(500).send("Erro interno: " + err.message);
  }
});

