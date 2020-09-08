import { Injectable } from '@angular/core';
import {
  ActivatedRouterStateSnapshot,
  ProductSearchPage,
  PageType,
} from '@spartacus/core';
import { ProductFacetService } from '@spartacus/storefront';

@Injectable()
export class VsProductFacetService extends ProductFacetService {
  protected filterForPage(
    state: ActivatedRouterStateSnapshot,
    page: ProductSearchPage
  ): boolean {
    // return true;
    if (
      state.context.type === PageType.CONTENT_PAGE &&
      state.context.id === 'search'
    ) {
      if (state.params.query.startsWith('visual-')) {
        return true;
      }
      return page.currentQuery.query.value.startsWith(`${state.params.query}:`);
    }
    return false;
  }
}
