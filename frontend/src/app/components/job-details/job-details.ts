import { JobService } from './../../services/job-service';
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { Job } from '../../model/job';
import { ArquivoRetorno } from '../../model/arquivo-retorno';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { ArquivoRetornoService } from '../../services/arquivo-retorno-service';
import { Subscription, switchMap, tap, finalize } from 'rxjs';
import { StatusArquivo } from '../../enum/status-arquivo';
import { StatusJobPipePipe } from '../../pipe/status-job-pipe-pipe';
import { StatusArquivoPipePipe } from '../../pipe/status-arquivo-pipe-pipe';
import { SseService } from '../../services/sse-service';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faEye, faTrash } from '@fortawesome/free-solid-svg-icons';
import { Transacao } from '../../model/transacao';
import { TransacaoService } from '../../services/transacao-service';
import { StatusJobTextoPipePipe } from '../../pipe/status-job-texto-pipe';
import { AlertService } from '../../services/alert-service';
import { ConfirmationService } from '../../services/confirmation-service';
import { FileNamePipe } from '../../pipe/file-name.pipe';

@Component({
  selector: 'app-job-details',
  imports: [
    FontAwesomeModule,
    CommonModule,
    RouterModule,
    StatusJobPipePipe,
    StatusArquivoPipePipe,
    StatusJobTextoPipePipe,
    DatePipe,
    FileNamePipe
  ],
  templateUrl: './job-details.html',
  styleUrl: './job-details.css',
})
export class JobDetails implements OnInit, OnDestroy {
  job: Job | null = null;
  arquivos: ArquivoRetorno[] = [];
  transacoesVisiveis: Transacao[] = [];
  arquivoSelecionadoId: number | null = null;
  loadingArquivos = false;
  loadingTransacoes = false;
  error = '';
  private subscription!: Subscription;
  faEye = faEye;
  faTrash = faTrash;

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private jobService = inject(JobService);
  private arquivoService = inject(ArquivoRetornoService);
  private transacaoService = inject(TransacaoService);
  private sseService = inject(SseService);
  private alertService = inject(AlertService);
  private confirmationService = inject(ConfirmationService);

  constructor() {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!isNaN(id)) {
      this.carregarDetalhes(id);
      this.iniciarEscutaSse();
    } else {
      this.alertService.show('Id do agendamento inválido.');
      this.router.navigate(['/']);
    }
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  carregarDetalhes(id: number) {
    this.loadingArquivos = true;
    this.jobService
      .buscarJobId(id)
      .pipe(
        tap((job) => (this.job = job)),
        switchMap(() => this.arquivoService.listarArquivoJobId(id)),
        finalize(() => (this.loadingArquivos = false))
      )
      .subscribe({
        next: (arquivos) => (this.arquivos = arquivos),
        error: (error) => {
          this.alertService.show(
            'Erro ao carregar detalhes ou arquivos do agendamento'
          );
          console.error(error);
        },
      });
  }

  visualizarTransacoes(arquivo: ArquivoRetorno): void {
    if (this.arquivoSelecionadoId === arquivo.id) {
      this.arquivoSelecionadoId = null;
      this.transacoesVisiveis = [];
      return;
    }
    this.arquivoSelecionadoId = arquivo.id;
    this.loadingTransacoes = true;
    this.transacaoService
      .getTransacoesPorArquivoId(arquivo.id)
      .pipe(finalize(() => (this.loadingTransacoes = false)))
      .subscribe({
        next: (transacoes) => (this.transacoesVisiveis = transacoes),
        error: (err) => {
          this.alertService.show('Erro ao buscar as transações do arquivo');
          console.error('Erro ao buscar transações', err);
        },
      });
  }

  iniciarEscutaSse(): void {
    this.subscription = this.sseService
      .getServerSentEvents('http://localhost:8080/api/sse/subscribe')
      .subscribe({
        next: (data) => this.handleSseData(data),
        error: (error) => {
          console.error('Erro SSE:', error);
        },
      });
  }

  private handleSseData(data: any): void {
    if (data && data.nomeArquivo) {
      const arquivoRecebido = data as ArquivoRetorno;
      if (this.job && arquivoRecebido.jobId === this.job.id) {
        const index = this.arquivos.findIndex(
          (a) => a.id === arquivoRecebido.id
        );
        if (index > -1) {
          this.arquivos[index] = arquivoRecebido;
        } else {
          this.arquivos.unshift(arquivoRecebido);
        }
      }
    } else if (data && data.cronExpression) {
      const jobRecebido = data as Job;
      if (this.job && jobRecebido.id === this.job.id) {
        this.job = jobRecebido;
      }
    }
    if (data && data.action === 'delete_arquivo' && data.arquivoId) {
      this.arquivos = this.arquivos.filter((a) => a.id !== data.arquivoId);

      if (this.arquivoSelecionadoId === data.arquivoId) {
        this.transacoesVisiveis = [];
        this.arquivoSelecionadoId = null;
      }
    }
  }

  async deletarArquivo(arquivo: ArquivoRetorno): Promise<void> {
    const mensagem = `Tem certeza que deseja excluir o arquivo "${arquivo.nomeArquivo}"?\n\nEsta ação não pode ser desfeita.`;
    const confirmacao = await this.confirmationService.confirm(mensagem);
    if (confirmacao) {
      this.arquivoService
        .deletarArquivoId(arquivo.id)
        .pipe(finalize(() => {}))
        .subscribe({
          next: () => {
            this.arquivos = this.arquivos.filter((a) => a.id !== arquivo.id);
            this.alertService.show('Arquivo deletado com sucesso!');
            console.log(`Arquivo ${arquivo.id} deletado com sucesso.`);
            if (this.arquivoSelecionadoId === arquivo.id) {
              this.transacoesVisiveis = [];
              this.arquivoSelecionadoId = null;
            }
          },
          error: (err) => {
            this.alertService.show('Erro ao deletar o arquivo!');
            console.error('Erro no delete:', err);
          },
        });
    }
  }

  voltar() {
    this.router.navigate(['/']);
  }
}
