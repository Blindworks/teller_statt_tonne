import { Component, signal } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { UserProfileDialogComponent } from './users/user-profile-dialog/user-profile-dialog.component';
import { StoreDetailDialogComponent } from './stores/store-detail-dialog/store-detail-dialog.component';
import { APP_VERSION } from './version';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, UserProfileDialogComponent, StoreDetailDialogComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('frontend');
  protected readonly version = APP_VERSION;
}
