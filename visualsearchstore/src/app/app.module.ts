import { NgModule } from '@angular/core';
import {
  BrowserModule,
  BrowserTransferStateModule,
} from '@angular/platform-browser';
import { StoreDevtoolsModule } from '@ngrx/store-devtools';
import { translationChunksConfig, translations } from '@spartacus/assets';
import { VisualSearchModule } from '@spartacus/cxlabs';
import { B2cStorefrontModule } from '@spartacus/storefront';
import { environment } from '../environments/environment';
import { AppComponent } from './app.component';

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule.withServerTransition({ appId: 'serverApp' }),
    B2cStorefrontModule.withConfig({
      backend: {
        occ: {
          baseUrl:
            'https://api.czfr-cxlabs1-d30-public.model-t.cc.commerce.ondemand.com',
          prefix: '/rest/v2/',
        },
      },
      context: {
        baseSite: ['visualsearch'],
        currency: ['USD'],
      },
      i18n: {
        resources: translations,
        chunks: translationChunksConfig,
        fallbackLang: 'en',
      },
      features: {
        level: '2.0',
      },
    }),
    BrowserTransferStateModule,
    VisualSearchModule,

    ...(environment.production ? [] : [StoreDevtoolsModule.instrument()]),
  ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule {}
