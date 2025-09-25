import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';

@Component({
  selector: 'app-novo-agendamento-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './novo-agendamento-modal.html',
  styleUrls: ['./novo-agendamento-modal.css'],
})
export class NovoAgendamentoModal {
  @Output() fechar = new EventEmitter<void>();
  @Output() salvar = new EventEmitter<{ nome: string; cronExpression: string }>();

  novoJob = { nome: '', cronExpression: '' };

  fecharModal() {
    this.fechar.emit();
  }

  onSubmit(form: NgForm) {
    if (form.invalid) {
      form.control.markAllAsTouched();
      return;
    }
    this.salvar.emit({ ...this.novoJob });
    form.resetForm();
    this.fechar.emit();
  }
}
