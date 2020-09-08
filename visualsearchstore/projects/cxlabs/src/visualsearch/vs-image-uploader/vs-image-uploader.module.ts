import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import {
  RoutingModule,
  UrlModule,
  ConfigModule,
  CmsConfig,
  provideDefaultConfig,
} from '@spartacus/core';
import { VisualSearchImageUploaderComponent } from './vs-image-uploader.component';
import { IconConfig, IconModule } from '@spartacus/storefront';

@NgModule({
  declarations: [VisualSearchImageUploaderComponent],
  imports: [
    CommonModule,
    IconModule,
    RouterModule,
    RoutingModule,
    UrlModule,
    ConfigModule.withConfig({
      icon: {
        symbols: {
          CAMERA: 'fas fa-camera',
        },
      },
    } as IconConfig),
  ],
  providers: [
    provideDefaultConfig({
      cmsComponents: {
        VisualSearchImageUploaderComponent: {
          component: VisualSearchImageUploaderComponent,
        },
      },
    } as CmsConfig),
  ],
  exports: [VisualSearchImageUploaderComponent],
})
export class VisualSearchImageUploaderModule {}
