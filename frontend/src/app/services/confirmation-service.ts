import { Injectable, signal } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ConfirmationService {
  public readonly isVisible = signal(false);
  public readonly message = signal('');

  private userResponse$ = new Subject<boolean>();

  public confirm(message: string): Promise<boolean> {
    this.message.set(message);
    this.isVisible.set(true);

    return new Promise(resolve => {
      const sub = this.userResponse$.subscribe(response => {
        resolve(response);
        sub.unsubscribe();
      });
    });
  }

  public onConfirm(): void {
    this.isVisible.set(false);
    this.userResponse$.next(true);
  }

  public onCancel(): void {
    this.isVisible.set(false);
    this.userResponse$.next(false);
  }
}