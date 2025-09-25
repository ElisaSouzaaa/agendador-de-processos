import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  public readonly alertMessage = signal<string | null>(null);
  public readonly isVisible = signal<boolean>(false);

  show(message: string): void {
    this.alertMessage.set(message);
    this.isVisible.set(true);
  }

  hide(): void {
    this.isVisible.set(false);
    this.alertMessage.set(null);
  }
}