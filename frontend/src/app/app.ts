import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { UserProfileDialogComponent } from './users/user-profile-dialog/user-profile-dialog.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, UserProfileDialogComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('frontend');
}
