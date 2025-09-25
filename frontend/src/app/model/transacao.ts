import { StatusArquivo } from "../enum/status-arquivo";

export interface Transacao {
  id: number;
  tipo: string;
  valor: number;
  data: string;
  descricao: string;
  codigoSaida: string;
  status: StatusArquivo;
}
