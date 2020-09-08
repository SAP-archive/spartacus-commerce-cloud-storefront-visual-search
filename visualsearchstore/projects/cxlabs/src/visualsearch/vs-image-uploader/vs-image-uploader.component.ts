import { Component } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { VisualSearchService } from '../visualsearch.service';

export type FileEventTarget = EventTarget & { files: FileList };

@Component({
  selector: 'cx-image-uploader',
  templateUrl: './vs-image-uploader.component.html',
  styleUrls: ['./vs-image-uploader.component.scss'],
})
export class VisualSearchImageUploaderComponent {
  // ensure that the image data is observer, as the routing logic is in the operations underneath.
  imageData$: Observable<boolean> = this.visualSearchService.imageData$.pipe(
    map((data) => !!data)
  );

  constructor(protected visualSearchService: VisualSearchService) {}

  /**
   * Saves the uploaded file.
   */
  upload(event: Event) {
    const file = (event.target as FileEventTarget).files[0];
    this.visualSearchService.save(file);
  }
}
