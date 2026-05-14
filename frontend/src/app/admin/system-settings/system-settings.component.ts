import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { SystemSetting, SystemSettingsService } from './system-settings.service';

interface SettingDef {
  key: string;
  label: string;
  help: string;
  min: number;
  max: number;
  unit: string;
}

const DEFINITIONS: SettingDef[] = [
  {
    key: 'hygiene.validity_months',
    label: 'Gültigkeitsdauer Hygienezertifikat',
    help: 'In Monaten. Ablaufdatum = Ausstellungsdatum + diese Anzahl Monate. Wirkt nur auf zukünftige Uploads.',
    min: 1,
    max: 60,
    unit: 'Monate',
  },
  {
    key: 'hygiene.warning_days_before',
    label: 'Vorwarnzeit vor Ablauf',
    help: 'In Tagen. So viele Tage vor Ablauf erhält der Retter eine Benachrichtigung & E-Mail.',
    min: 1,
    max: 365,
    unit: 'Tage',
  },
];

interface SettingRow {
  def: SettingDef;
  form: FormGroup<{ value: FormControl<string> }>;
  saved: SystemSetting | null;
  saving: boolean;
  error: string | null;
  success: string | null;
}

@Component({
  selector: 'app-system-settings',
  standalone: true,
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="pt-24 pb-32 md:pb-12 px-4 md:pl-80 md:pr-12 max-w-3xl mx-auto min-h-screen">
      <h1 class="text-2xl md:text-4xl font-extrabold text-primary mb-2">Systemeinstellungen</h1>
      <p class="text-sm text-on-surface-variant mb-8">
        Werte gelten systemweit. Änderungen wirken sofort auf neue Uploads und auf den nächtlichen
        Warnungs-Lauf.
      </p>

      @if (loading()) {
        <p class="text-sm text-on-surface-variant">Lade Einstellungen…</p>
      } @else {
        <div class="space-y-6">
          @for (row of rows(); track row.def.key) {
            <form
              [formGroup]="row.form"
              (ngSubmit)="save(row)"
              class="rounded-xl bg-surface-container-lowest p-6 space-y-3"
            >
              <div>
                <h2 class="text-base font-bold text-on-surface">{{ row.def.label }}</h2>
                <p class="text-xs text-on-surface-variant mt-1">{{ row.def.help }}</p>
              </div>
              <div class="flex flex-wrap items-center gap-3">
                <input
                  type="number"
                  formControlName="value"
                  [min]="row.def.min"
                  [max]="row.def.max"
                  class="bg-surface-container-low rounded-full px-5 py-3 text-sm w-32 focus:outline-none focus:ring-2 focus:ring-primary"
                />
                <span class="text-sm text-on-surface-variant">{{ row.def.unit }}</span>
                <button
                  type="submit"
                  [disabled]="row.saving || row.form.invalid || row.form.pristine"
                  class="bg-primary text-on-primary px-5 py-2 rounded-full font-bold text-sm disabled:opacity-50"
                >
                  {{ row.saving ? 'Speichert…' : 'Speichern' }}
                </button>
                @if (row.saved?.updatedAt) {
                  <span class="text-xs text-on-surface-variant">
                    zuletzt geändert {{ row.saved!.updatedAt }}
                  </span>
                }
              </div>
              @if (row.error) {
                <div class="text-sm text-error font-semibold">{{ row.error }}</div>
              }
              @if (row.success) {
                <div class="text-sm text-primary font-semibold">{{ row.success }}</div>
              }
            </form>
          }
        </div>
      }
    </main>
  `,
})
export class SystemSettingsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(SystemSettingsService);

  readonly loading = signal(true);
  readonly settings = signal<SystemSetting[]>([]);

  readonly rows = computed<SettingRow[]>(() => {
    const map = new Map(this.settings().map((s) => [s.key, s]));
    return this.rowSignals();
  });

  private readonly rowSignals = signal<SettingRow[]>([]);

  ngOnInit(): void {
    this.service.list().subscribe({
      next: (list) => {
        this.settings.set(list);
        const map = new Map(list.map((s) => [s.key, s]));
        const rows: SettingRow[] = DEFINITIONS.map((def) => {
          const saved = map.get(def.key) ?? null;
          const initial = saved?.value ?? '';
          return {
            def,
            form: this.fb.group({
              value: this.fb.nonNullable.control(initial, [
                Validators.required,
                Validators.pattern(/^[0-9]+$/),
              ]),
            }),
            saved,
            saving: false,
            error: null,
            success: null,
          };
        });
        this.rowSignals.set(rows);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  save(row: SettingRow): void {
    const raw = row.form.controls.value.value;
    const n = Number(raw);
    if (!Number.isFinite(n) || n < row.def.min || n > row.def.max) {
      row.error = `Wert muss zwischen ${row.def.min} und ${row.def.max} liegen.`;
      this.touch();
      return;
    }
    row.saving = true;
    row.error = null;
    row.success = null;
    this.touch();
    this.service.update(row.def.key, String(n)).subscribe({
      next: (saved) => {
        row.saving = false;
        row.saved = saved;
        row.success = 'Gespeichert.';
        row.form.markAsPristine();
        this.touch();
      },
      error: (err) => {
        row.saving = false;
        row.error = typeof err?.error === 'string' ? err.error : 'Speichern fehlgeschlagen.';
        this.touch();
      },
    });
  }

  private touch(): void {
    this.rowSignals.set([...this.rowSignals()]);
  }
}
