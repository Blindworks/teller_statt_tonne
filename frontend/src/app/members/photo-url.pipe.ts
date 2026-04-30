import { Pipe, PipeTransform } from '@angular/core';
import { resolvePhotoUrl } from './photo-url';

@Pipe({ name: 'photoUrl', standalone: true })
export class PhotoUrlPipe implements PipeTransform {
  transform(value: string | null | undefined): string | null {
    return resolvePhotoUrl(value);
  }
}
