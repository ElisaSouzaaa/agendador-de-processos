import { Pipe, PipeTransform } from '@angular/core';
import { StatusArquivo } from '../enum/status-arquivo';

@Pipe({
  name: 'statusArquivoPipe',
})
export class StatusArquivoPipePipe implements PipeTransform {
  transform(status: StatusArquivo): string {
    switch (status) {
      case StatusArquivo.pendente:
        return 'status-pendente';
      case StatusArquivo.processado:
        return 'status-processado';
      case StatusArquivo.concluido_com_erros:
        return 'status-concluido-com-erros';
      case StatusArquivo.erro:
        return 'status-erro';
      default:
        return 'Erro inesperado!';
    }

    return '';
  }
}
