export type Dispositivo = {
  id: string;
  companyId: string;
  establishmentId: string;
  terminalId: string | null;
  fingerprintHash: string | null;
  certificateThumbprint: string | null;
  agentVersion: string | null;
  status: string;
  registeredAt: string;
  lastContactAt: string | null;
};
