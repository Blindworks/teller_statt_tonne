import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-imprint',
  imports: [RouterLink],
  templateUrl: './imprint.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImprintComponent {}
