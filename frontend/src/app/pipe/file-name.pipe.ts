import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'fileName'
})
export class FileNamePipe implements PipeTransform {
  transform(fullPath: string): string {
    if (!fullPath) {
      return '';
    }
    return fullPath.split('/').pop() || fullPath;
  }
}
