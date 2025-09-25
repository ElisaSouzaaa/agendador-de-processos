import { Pipe, PipeTransform } from '@angular/core';
import { StatusJob } from '../enum/status-job';

@Pipe({
  name: 'statusJobTextoPipe',
  standalone: true,
})
export class StatusJobTextoPipePipe implements PipeTransform {
  transform(status: StatusJob): string {
    switch (status) {
      case StatusJob.agendado:
        return 'Agendado';
      case StatusJob.processando:
        return 'Processando';
      case StatusJob.concluido:
        return 'Concluído';
      case StatusJob.concluido_com_erros:
        return 'Concluído com Erros';
      case StatusJob.falha:
        return 'Falha';
      default:
        return 'Status Desconhecido';
    }
  }
}