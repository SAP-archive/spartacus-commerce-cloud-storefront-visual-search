import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import {
  ConfigModule,
  PageMetaResolver,
  RoutingModule,
  UrlModule,
} from '@spartacus/core';
import {
  IconModule,
  ProductFacetService,
  ProductListComponentService,
} from '@spartacus/storefront';
import { VisualSearchProductListComponentService } from './product-list-component.service';
import { VisualSearchConfig } from './visualsearch.config';
import { VisualSearchService } from './visualsearch.service';
import { VisualSearchTranslations } from './visualsearch.translations';
import { VisualSearchImageHolderModule } from './vs-image-holder/vs-image-holder.module';
import { VisualSearchImageUploaderModule } from './vs-image-uploader/vs-image-uploader.module';
import { VsProductFacetService } from './vs-product-facet.service';
import { VisualSearchPageMetaResolver } from './vs-search-page.meta.resolver';

export const visualSearchConfig: VisualSearchConfig = {
  visualSearchProvider: {
    url: 'https://vs.ikick.de/imageservice',
  },
};

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    IconModule,
    RouterModule,
    RoutingModule,
    UrlModule,
    HttpClientModule,
    VisualSearchImageHolderModule,
    VisualSearchImageUploaderModule,
    ConfigModule.withConfig({
      i18n: {
        resources: VisualSearchTranslations,
        fallbackLang: 'en',
      },
    }),
  ],
  providers: [
    VisualSearchService,
    {
      provide: VisualSearchConfig,
      useValue: visualSearchConfig,
    },
    {
      provide: ProductListComponentService,
      useClass: VisualSearchProductListComponentService,
    },
    {
      provide: ProductFacetService,
      useClass: VsProductFacetService,
    },
    {
      provide: PageMetaResolver,
      useExisting: VisualSearchPageMetaResolver,
      multi: true,
    },
  ],
})
export class VisualSearchModule {}
