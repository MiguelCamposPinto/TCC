const admin = require('firebase-admin');
const { Pool } = require('pg');
const path = require('path');

const serviceAccountPath = path.resolve(__dirname, 'serviceAccountKey.json');
admin.initializeApp({
  credential: admin.credential.cert(require(serviceAccountPath)),
});
const db = admin.firestore();

const pool = new Pool({
  host: 'ep-lingering-darkness-ac74wunk-pooler.sa-east-1.aws.neon.tech',
  port: 5432,
  database: 'neondb',
  user: 'neondb_owner',
  password: 'npg_z5RxvhwW3CTo',
  ssl: require,
});

// DDL – igual ao que você já usou
const ensureTableSQL = `
CREATE TABLE IF NOT EXISTS reservas (
  id             TEXT PRIMARY KEY,
  building_id    TEXT        NOT NULL,
  date           DATE        NOT NULL,
  duration_min   INTEGER,
  start_time     TIME        NOT NULL,
  end_time       TIME        NOT NULL,
  status         TEXT        NOT NULL,
  firestore_path TEXT,
  machine_id     TEXT        NOT NULL,
  machine_name   TEXT,
  space_id       TEXT        NOT NULL,
  space_name     TEXT,
  space_type     TEXT,
  user_id        TEXT        NOT NULL,
  user_name      TEXT,
  created_at     TIMESTAMPTZ DEFAULT NOW(),
  updated_at     TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_res_building_date ON reservas (building_id, date);
CREATE INDEX IF NOT EXISTS idx_res_machine       ON reservas (machine_id);
CREATE INDEX IF NOT EXISTS idx_res_user          ON reservas (user_id);
`;

function toTime(value) {
  if (!value || typeof value !== 'string') return null; // espere "HH:MM"
  return value;
}
function toDateOnly(value) {
  if (!value || typeof value !== 'string') return null; // espere "YYYY-MM-DD"
  return value;
}

async function migrate() {
  const client = await pool.connect();
  try {
    console.log('Conectando ao PostgreSQL e garantindo tabela...');
    await client.query(ensureTableSQL);

    // Coleções de recursos por espaço: lavanderia (machines), quadras, salões
    const resourceCollections = ['machines', 'quadras', 'saloes'];

    console.log('Lendo prédios do Firestore...');
    const buildingsSnap = await db.collection('buildings').get();

    for (const buildingDoc of buildingsSnap.docs) {
      const buildingId = buildingDoc.id;

      const spacesSnap = await db.collection(`buildings/${buildingId}/spaces`).get();
      for (const spaceDoc of spacesSnap.docs) {
        const spaceId = spaceDoc.id;

        for (const resourceType of resourceCollections) {
          // Ex.: buildings/{b}/spaces/{s}/quadras
          const resourcesSnap = await db
            .collection(`buildings/${buildingId}/spaces/${spaceId}/${resourceType}`)
            .get();

          for (const resourceDoc of resourcesSnap.docs) {
            const resourceId = resourceDoc.id;

            // Ex.: buildings/{b}/spaces/{s}/quadras/{resourceId}/reservations
            const reservasSnap = await db
              .collection(
                `buildings/${buildingId}/spaces/${spaceId}/${resourceType}/${resourceId}/reservations`
              )
              .get();

            for (const reservaDoc of reservasSnap.docs) {
              const reserva = reservaDoc.data();
              const id = reservaDoc.id;

              const date = toDateOnly(reserva.date);
              const durationMin = reserva.durationMin ?? null;
              const startTime = toTime(reserva.startTime);
              const endTime = toTime(reserva.endTime);
              const status = reserva.status || 'indefinido';

              const firestorePath = reserva.firestorePath ?? null;

              // Alguns docs já trazem esses nomes; se não vier, tenta do recurso
              const machineName = reserva.machineName ?? resourceDoc.get('name') ?? null;
              const spaceName = reserva.spaceName ?? spaceDoc.get('name') ?? null;

              // spaceType vem no doc (lavanderias/quadras/saloes). Se não vier, usamos o nome da coleção.
              const spaceType = reserva.spaceType ?? resourceType;

              const userId = reserva.userId || '';
              const userName = reserva.userName ?? null;

              try {
                await client.query(
                  `
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
                  `,
                  [
                    id, buildingId, date, durationMin, startTime, endTime, status,
                    firestorePath, resourceId, machineName, spaceId, spaceName, spaceType,
                    userId, userName
                  ]
                );
              } catch (err) {
                console.error('Erro ao inserir/atualizar:', id, err.message);
              }
            }
          }
        }
      }
    }

    console.log('Migração finalizada.');
  } catch (e) {
    console.error('Falha na migração:', e);
  } finally {
    client.release();
    await pool.end();
  }
}

migrate().catch((e) => {
  console.error(e);
  process.exit(1);
});
