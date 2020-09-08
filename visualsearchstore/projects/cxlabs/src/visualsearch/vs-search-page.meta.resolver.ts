import {
  PageMetaResolver,
  RoutingService,
  ProductSearchService,
  TranslationService,
  PageType,
} from '@spartacus/core';
import { Injectable } from '@angular/core';
import { Observable, combineLatest } from 'rxjs';
import { switchMap, map, filter } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class VisualSearchPageMetaResolver extends PageMetaResolver
  implements PageMetaResolver {
  protected total$: Observable<
    number
  > = this.productSearchService.getResults().pipe(
    filter((data) => !!data?.pagination),
    map((results) => results.pagination.totalResults)
  );

  protected query$: Observable<
    string
  > = this.routingService
    .getRouterState()
    .pipe(map((state) => state.state.params['query']));

  constructor(
    protected routingService: RoutingService,
    protected productSearchService: ProductSearchService,
    protected translation: TranslationService
  ) {
    super();
    this.pageType = PageType.CONTENT_PAGE;
    this.pageTemplate = 'SearchResultsListPageTemplate';
  }

  resolveTitle(): Observable<string> {
    const sources = [this.total$, this.query$];
    return combineLatest(sources).pipe(
      switchMap(([t, q]: [number, string]) =>
        this.translation.translate('pageMetaResolver.search.title', {
          count: t,
          query: q.startsWith('visual-') ? 'visual-search' : q,
        })
      )
    );
  }
}
