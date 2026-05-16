import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  signal,
} from '@angular/core';

@Component({
  selector: 'app-live-clock',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="tabular-nums font-mono tracking-wider">{{ time() }}</span>
  `,
})
export class LiveClockComponent {
  readonly time = signal(LiveClockComponent.format(new Date()));

  constructor() {
    const handle = setInterval(() => this.time.set(LiveClockComponent.format(new Date())), 1000);
    inject(DestroyRef).onDestroy(() => clearInterval(handle));
  }

  private static format(d: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  }
}
