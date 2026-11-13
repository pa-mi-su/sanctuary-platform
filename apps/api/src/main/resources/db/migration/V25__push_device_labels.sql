ALTER TABLE push_devices ADD COLUMN device_label TEXT CHECK (length(device_label) <= 100);
