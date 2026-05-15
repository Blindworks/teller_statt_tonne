import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-test-user-badge',
  standalone: true,
  template: `<span
    class="inline-flex items-center rounded-full bg-amber-100 px-2 py-0.5 text-xs font-semibold uppercase tracking-wider text-amber-800 ring-1 ring-amber-300"
    title="Test-Retter"
    >Test</span
  >`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TestUserBadgeComponent {}
