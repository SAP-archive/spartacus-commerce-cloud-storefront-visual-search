import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';
import {
  RoutingModule,
  UrlModule,
  CmsConfig,
  provideDefaultConfig,
} from '@spartacus/core';
import { VisualSearchImageUploaderComponent } from './vs-image-holder.component';

@NgModule({
  declarations: [VisualSearchImageUploaderComponent],
  imports: [
    CommonModule,
    RouterModule,
    RoutingModule,
    UrlModule,
    HttpClientModule,
  ],
  providers: [
    provideDefaultConfig({
      cmsComponents: {
        VisualSearchImageHolderComponent: {
          component: VisualSearchImageUploaderComponent,
        },
      },
    } as CmsConfig),
  ],
  exports: [VisualSearchImageUploaderComponent],
})
export class VisualSearchImageHolderModule {}
