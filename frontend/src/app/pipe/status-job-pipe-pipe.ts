import { Pipe, PipeTransform } from '@angular/core';
import { StatusJob } from '../enum/status-job';

@Pipe({
  name: 'statusJobPipe',
})
export class StatusJobPipePipe implements PipeTransform {
  transform(status: StatusJob): string {
    switch (status) {
      case StatusJob.agendado:
        return 'status-agendado';
      case StatusJob.processando:
        return 'status-processando';
      case StatusJob.concluido:
        return 'status-concluido';
      case StatusJob.concluido_com_erros:
        return 'status-concluido-com-erros';
      case StatusJob.falha:
        return 'status-falha';
      default:
        return 'Erro inesperado!';
    }
  }
}
