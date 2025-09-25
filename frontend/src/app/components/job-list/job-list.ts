import { Component, inject, NgZone, OnDestroy, OnInit, ChangeDetectorRef } from '@angular/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faPenToSquare,
  faEye,
  faTrash,
  faUpload,
  faSearch,
} from '@fortawesome/free-solid-svg-icons';
import { Job } from '../../model/job';
import { JobService } from '../../services/job-service';
import { Router, RouterModule } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { StatusJobPipePipe } from '../../pipe/status-job-pipe-pipe';
import { HttpClient } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { finalize, Subscription } from 'rxjs';
import { SseService } from '../../services/sse-service';
import { ArquivoRetornoService } from '../../services/arquivo-retorno-service';
import { FormsModule } from '@angular/forms';
import { StatusJobTextoPipePipe } from '../../pipe/status-job-texto-pipe';
import { StatusJob } from '../../enum/status-job';
import { UploadModal } from '../upload-modal/upload-modal';
import { NovoAgendamentoModal } from '../novo-agendamento-modal/novo-agendamento-modal';
import { AlertService } from '../../services/alert-service';
import { ConfirmationService } from '../../services/confirmation-service';

@Component({
  selector: 'app-job-list',
  imports: [
    FontAwesomeModule,
    CommonModule,
    RouterModule,
    FormsModule,
    StatusJobPipePipe,
    StatusJobTextoPipePipe,
    DatePipe,
    NovoAgendamentoModal,
  ],
  templateUrl: './job-list.html',
  styleUrls: ['./job-list.css'],
})
export class JobList implements OnInit, OnDestroy {
  faPen = faPenToSquare;
  faEye = faEye;
  faTrash = faTrash;
  faUpload = faUpload;
  faSearch = faSearch;
  mostrarModalUpload = false;
  jobSelecionadoParaUpload: Job | null = null;
  pingPongVisual: { [jobId: number]: StatusJob | null } = {};
  arquivoSelecionado: File | null = null;
  mensagemErroUpload = '';
  termoBuscaArquivo = '';
  private http = inject(HttpClient);
  private dialog = inject(MatDialog);
  private jobService = inject(JobService);
  private router = inject(Router);
  private sseService = inject(SseService);
  private arquivoRetornoService = inject(ArquivoRetornoService);
  private alertService = inject(AlertService);
  private confirmationService = inject(ConfirmationService);
  private zone = inject(NgZone);
  private cdr = inject(ChangeDetectorRef);
  private subscription!: Subscription;
  jobs: Job[] = [];
  loading = false;
  mostrarModalNovo = false;
  error = '';
  pingPongStatus: { [jobId: number]: boolean } = {};

  constructor() {}

  ngOnInit(): void {
    this.carregarJobs();
    this.iniciarEscutaSse();
  }

  iniciarEscutaSse(): void {
    this.subscription = this.sseService
      .getServerSentEvents('http://localhost:8080/api/sse/subscribe')
      .subscribe({
        next: (data) => this.handleSseData(data),
        error: (err) => {
          console.error('SSE Error:', err);
          this.alertService.show('Erro de conexão com o servidor');
          this.error = '';
        },
      });
  }

  private handleSseData(data: any): void {
    this.zone.run(() => {
      if (data && data.action === 'delete' && data.jobId) {
        this.jobs = this.jobs.filter((job) => job.id !== data.jobId);
        this.cdr.detectChanges();
        return;
      }
      if (data && data.cronExpression) {
        const jobData = data as Job;
        const index = this.jobs.findIndex((job) => job.id === jobData.id);
        if (index > -1) {
          this.jobs[index] = jobData;
        } else {
          this.jobs.unshift(jobData);
        }
        this.jobs = [...this.jobs];
        this.iniciarPingPongVisual(jobData);
        this.cdr.detectChanges();
      }
    });
  }

  private iniciarPingPongVisual(job: Job) {
    if (this.pingPongStatus[job.id]) return;
    this.pingPongStatus[job.id] = true;
    const loopPingPong = async () => {
      try {
        while (this.jobs.find((j) => j.id === job.id)) {
          const jobAtual = this.jobs.find((j) => j.id === job.id);
          if (!jobAtual) break;
          const statusReal = jobAtual.status;
          const temArquivo = statusReal !== StatusJob.agendado;
          this.zone.run(() => {
            this.pingPongVisual[job.id] = StatusJob.processando;
            this.cdr.detectChanges();
          });
          await new Promise((res) => setTimeout(res, temArquivo ? 4000 : 5000));
          this.zone.run(() => {
            this.pingPongVisual[job.id] = statusReal;
            this.cdr.detectChanges();
          });
          await new Promise((res) => setTimeout(res, temArquivo ? 4000 : 5000));
        }
      } catch (e) {
        console.error('Erro no ping-pong visual', e);
      } finally {
        this.zone.run(() => {
          delete this.pingPongStatus[job.id];
          delete this.pingPongVisual[job.id];
          this.cdr.detectChanges();
        });
      }
    };
    this.zone.runOutsideAngular(() => loopPingPong());
  }

  carregarJobs() {
    this.loading = true;
    this.jobService
      .listarJobs()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (data) => {
          this.jobs = [...data];
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Erro ao carregar jobs:', err);
          this.alertService.show('Erro ao carregar agendamentos.');
        },
      });
  }

  abrirModalUpload(job: Job): void {
    this.dialog.open(UploadModal, {
      width: '550px',
      data: { jobId: job.id, jobName: job.nome },
      panelClass: 'custom-dialog-container',
    });
  }

  fecharModalUpload() {
    this.mostrarModalUpload = false;
    this.jobSelecionadoParaUpload = null;
    this.arquivoSelecionado = null;
    this.mensagemErroUpload = '';
  }

  onArquivoSelecionado(event: Event) {
    const element = event.target as HTMLInputElement;
    if (element.files && element.files.length > 0) {
      const file = element.files[0];
      if (file.type === 'text/plain' || file.name.endsWith('.txt')) {
        this.arquivoSelecionado = file;
      } else {
        this.alertService.show('Por favor, selecione um arquivo .txt válido');
        this.arquivoSelecionado = null;
      }
    }
  }

  confirmarUpload() {
    if (!this.arquivoSelecionado || !this.jobSelecionadoParaUpload) {
      this.alertService.show('Selecione um arquivo para enviar');
      this.iniciarEscutaSse();
      return;
    }
    this.loading = true;
    this.arquivoRetornoService
      .uploadoArquivo(this.jobSelecionadoParaUpload.id, this.arquivoSelecionado)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (resposta) => {
          console.log('Resposta do servidor:', resposta);
          this.alertService.show(
            'Arquivo enviado com sucesso! O status do agendamento será atualizado em breve.'
          );
          this.fecharModalUpload();
          this.carregarJobs();
        },
        error: (erro) => {
          this.alertService.show('Erro ao enviar o arquivo. Tente novamente');
          this.carregarJobs();
          console.error('Erro no upload:', erro);
        },
      });
  }

  abrirModalNovo() {
    this.mostrarModalNovo = true;
  }

  fecharModalNovo() {
    this.mostrarModalNovo = false;
  }

  criarAgendamento(novoJob: { nome: string; cronExpression: string }) {
    this.loading = true;
    this.jobService
      .criarJob(novoJob)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (jobCriado) => {
          this.alertService.show('Novo agendamento criado com sucesso!');
          this.mostrarModalNovo = false;
        },
        error: (err) => {
          console.error('Erro ao criar job:', err);
          this.alertService.show('Erro ao criar agendamento. Tente novamente.');
          this.carregarJobs();
        },
      });
  }

  buscarArquivo(): void {
    const jobId = parseInt(this.termoBuscaArquivo, 10);
    if (isNaN(jobId)) {
      this.alertService.show(
        'Por favor, digite um ID numérico válido de agendamento.'
      );
      return;
    }
    this.loading = true;
    this.jobService
      .buscarJobId(jobId)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (jobEncontrado) => {
          this.alertService.show(
            `Agendamento encontrado! Redirecionando para os detalhes do agendamento: ${jobEncontrado.nome}`
          );
          this.router.navigate(['/detalhes', jobEncontrado.id]);
        },
        error: (err) => {
          if (err.status === 404) {
            this.alertService.show(`Nenhum agendamento encontrado com o ID: ${jobId}`);
          } else {
            this.alertService.show('Ocorreu um erro ao buscar o agendamento.');
          }
          console.error('Erro na busca do agendamento:', err);
        },
      });
  }

  editar(id: number) {
    this.router.navigate(['/editar', id]);
  }

  detalhes(id: number) {
    this.router.navigate(['/detalhes', id]);
  }

  async excluir(id: number): Promise<void> {
    const mensagem = 'Deseja realmente excluir este agendamento?';
    const confirmacao = await this.confirmationService.confirm(mensagem);
    if (confirmacao) {
      this.loading = true;
      this.jobService
        .deletarJob(id)
        .pipe(finalize(() => (this.loading = false)))
        .subscribe({
          next: () => {
            this.jobs = this.jobs.filter((job) => job.id !== id);
            this.alertService.show('Agendamento deletado com sucesso!');
            this.cdr.detectChanges();
          },
          error: () => {
            this.alertService.show('Erro ao deletar agendamento!');
            this.carregarJobs();
          },
        });
    }
  }

  novo() {
    this.router.navigate(['/novo']);
  }

  trackByJobId(index: number, job: Job) {
    return job.id;
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }
}
