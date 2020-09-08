import { Component, ViewEncapsulation } from '@angular/core';
import { RoutingService } from '@spartacus/core';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { VisualSearchService } from '../visualsearch.service';

@Component({
  selector: 'cx-image-holder',
  templateUrl: './vs-image-holder.component.html',
  styleUrls: ['./vs-image-holder.component.scss'],
  encapsulation: ViewEncapsulation.None,
})
export class VisualSearchImageUploaderComponent {
  protected isVisualImageRoute$: Observable<
    boolean
  > = this.routingService
    .getRouterState()
    .pipe(
      map(
        (state) =>
          state.state.params.query &&
          state.state.params.query.startsWith('visual-')
      )
    );

  imageData$: Observable<{
    isVisualSearchRoute: boolean;
    data: any;
  }> = combineLatest([
    this.isVisualImageRoute$,
    this.visualSearchService.imageData$,
  ]).pipe(
    map(([isVisualSearchRoute, data]) => ({
      isVisualSearchRoute,
      data,
    }))
  );

  constructor(
    protected visualSearchService: VisualSearchService,
    protected routingService: RoutingService
  ) {}

  /**
   * Get similar product ids
   * @param id - id of the item on image
   */
  getSimilar(id: string) {
    this.visualSearchService.getSimilarProducts(id);
  }
}
