import { TranslationResources } from '@spartacus/core';

export const VisualSearchTranslationsChunkName = 'imageUploader';

export const imageUploader = {
  imageUploader: {
    uploading: 'Uploading...',
    uploadingStatusError: 'Error while uploading image. Please try again',
    uploadingStatusEmptyResults: 'No detected items on image',
  },
};

export const VisualSearchTranslations: TranslationResources = {
  en: {
    imageUploader,
  },
};
