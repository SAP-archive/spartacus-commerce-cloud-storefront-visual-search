import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import {
  GlobalMessageService,
  GlobalMessageType,
  RoutingService,
} from '@spartacus/core';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { catchError, filter, map, take, tap } from 'rxjs/operators';
import { VisualSearchConfig } from './visualsearch.config';
import { VisualSearchImageData } from './visualsearch.model';

interface FileUploadResult {
  file: File;
  result: VisualSearchImageData;
}

@Injectable({
  providedIn: 'root',
})
export class VisualSearchService {
  private fileUpload$ = new BehaviorSubject<FileUploadResult>(undefined);

  imageData$: Observable<any> = this.fileUpload$.asObservable().pipe(
    filter((x) => Boolean(x)),
    tap((data) => this.handleMatchError(data.result)),
    map((data) => {
      return {
        source: this.sanitizer.bypassSecurityTrustUrl(
          URL.createObjectURL(data.file)
        ),
        bb: data.result.boundingBoxes.map((val) => ({
          title: val.label,
          left: val.x1 * 100,
          top: val.y1 * 100,
          width: (val.x2 - val.x1) * 100,
          height: (val.y2 - val.y1) * 100,
          id: val.id,
        })),
      };
    })
  );

  constructor(
    protected http: HttpClient,
    protected config: VisualSearchConfig,
    protected routingService: RoutingService,
    protected sanitizer: DomSanitizer,
    protected globalMessageService: GlobalMessageService
  ) {}

  protected launchVisualSearchPage(uid: string): void {
    this.globalMessageService.remove(GlobalMessageType.MSG_TYPE_INFO);
    this.routingService.go({
      cxRoute: 'search',
      params: {
        query: `visual-${uid}`,
        sortCode: 'visual-relevance',
      },
    });
  }

  save(image: File) {
    this.globalMessageService.add(
      { key: 'imageUploader.uploading' },
      GlobalMessageType.MSG_TYPE_INFO,
      300000
    );
    this.upload(image)
      .pipe(take(1))
      .subscribe((result) => {
        this.fileUpload$.next({ file: image, result });
        this.launchVisualSearchPage(result.boundingBoxes[0].id);
      });
  }

  protected upload(image: File): Observable<VisualSearchImageData> {
    const url = this.config.visualSearchProvider.url + '/upload';
    const fd = new FormData();
    fd.append('file', image);

    return this.http.post<VisualSearchImageData>(url, fd).pipe(
      catchError((error: any) => {
        console.error(error);
        this.globalMessageService.remove(GlobalMessageType.MSG_TYPE_INFO);
        this.globalMessageService.add(
          { key: 'imageUploader.uploadingStatusError' },
          GlobalMessageType.MSG_TYPE_ERROR
        );
        return throwError(error);
      })
    );
  }

  protected handleMatchError(data) {
    if (!data || !data.boundingBoxes || data.boundingBoxes.length === 0) {
      this.globalMessageService.remove(GlobalMessageType.MSG_TYPE_INFO);
      this.globalMessageService.add(
        { key: 'imageUploader.uploadingStatusEmptyResults' },
        GlobalMessageType.MSG_TYPE_ERROR
      );
    }
  }

  getSimilarProducts(id: string): Observable<string[]> {
    const url = this.config.visualSearchProvider.url + '/' + id;
    return this.http.get<string[]>(url);
  }
}
