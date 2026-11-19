CREATE TABLE push_devices (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 installation_id UUID NOT NULL UNIQUE,
 token TEXT NOT NULL UNIQUE CHECK(length(token) BETWEEN 20 AND 4096),
 platform TEXT NOT NULL CHECK(platform IN ('ios','android')),
 app_version TEXT NOT NULL,
 enabled BOOLEAN NOT NULL DEFAULT false,
 valid BOOLEAN NOT NULL DEFAULT true,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE push_messages (
 id UUID PRIMARY KEY,
 actor_sub TEXT NOT NULL,
 title TEXT NOT NULL,
 body TEXT NOT NULL,
 destination TEXT NOT NULL,
 audience TEXT NOT NULL CHECK(audience IN ('all','ios','android','test')),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE push_deliveries (
 message_id UUID NOT NULL REFERENCES push_messages(id),
 device_id UUID REFERENCES push_devices(id) ON DELETE SET NULL,
 recipient_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 state TEXT NOT NULL DEFAULT 'pending' CHECK(state IN ('pending','sending','accepted','failed','unknown','skipped')),
 provider_id TEXT,
 error_code TEXT,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(message_id,device_id)
);
CREATE INDEX push_deliveries_pending ON push_deliveries(state,updated_at);
