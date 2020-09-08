# Visual Search Spartacus Components Internals

The file structure for the mystore example in src/app has the following structure:

```text
├── app.component.html
├── app.component.scss
├── app.component.spec.ts
├── app.component.ts
├── app.module.ts
├── image-holder
│   ├── image-holder.component.html
│   ├── image-holder.component.scss
│   ├── image-holder.component.ts
│   └── utils.ts
├── image-uploader
│   ├── image-uploader.component.html
│   ├── image-uploader.component.scss
│   └── image-uploader.component.ts
└── visual-search
    ├── visual-search-config.ts
    ├── visual-search.model.ts
    ├── visual-search.module.ts
    └── visual-search.service.ts
```

The Visual Search components are based on the image-uploader and image-holder components and the visual-search service.

## extend your Spartacus project

To extend your Spartacus project you can extend the `SearchBoxComponent` and `BreadcrumbComponent` in the `app.component.html` file of your project:

```xml
<ng-template cxOutletRef="SearchBoxComponent" cxOutletPos="after">
  <image-uploader></image-uploader>
</ng-template>

<ng-template cxOutletRef="BreadcrumbComponent" cxOutletPos="after">
  <image-holder></image-holder>
</ng-template>
```

The search box component is extended by the image-uploader component and the breadcrumb component by the image-holder component.

## image uploader component

The image uploader component extend the search box with a camera symbol (as cx-icon) to upload a picture.

![image uploader component with camera symbol](images/image-uploader-component.png)

When the user clicks the button, a dialog opens to select a file or to take a photo (on mobile devices). The image upload component uploads the image by calling the visual search service.

### visual search service

The visual search service is an Angular Service. It uploads the image to the Visual Search microservice and notifies the image holder component.

## image holder component

The Image Holder component shows the uploaded image and the bounding boxes for the found categories. In addition, the bounding boxes can be used to perform a product search by clicking on them.

The following code (from the image-holder.component.html file) shows the possible parameters for each bounding box:

```HTML
<a
  *ngFor="let rect of imageData.bb"
  [title]="rect.title"
  [style.left.%]="rect.left"
  [style.top.%]="rect.top"
  [style.width.%]="rect.width"
  [style.height.%]="rect.height"
  (click)="getSimilar(rect.id)"
></a>
```

rect parameters:

- __title__; catagory or title of the identified product
- __left__: left possition of the bounding box in percentage
- __top__ :top possition of the bounding box in percentage
- __width__: width of the bounding box in percentage
- __height__: height possition of the bounding box in percentage
- __id__: the ID of the identified product which is used to find similar products

### bounding boxes

By clicking on a bounding box, the visual search service calls the Visual Search microservice with the ID which calls the 3rd party provider to receive similar products for the selected product. The list of similar products is used for the search. The search page is displayed.
