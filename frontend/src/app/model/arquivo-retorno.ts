import { StatusArquivo } from "../enum/status-arquivo";

export interface ArquivoRetorno {
  id: number;
  jobId: number;
  nomeArquivo: string;
  dataProcessamento: Date;
  status: StatusArquivo;
  cabecalhoNumerico: string;
  cabecalhoTexto: string;
  cabecalhoCodigo: string;
}
