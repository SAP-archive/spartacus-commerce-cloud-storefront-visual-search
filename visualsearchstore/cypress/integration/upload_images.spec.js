/// <reference types="cypress" />

context('Window', () => {
  beforeEach(() => {
    cy.visit('http://localhost:4200')
  })

  it('cy.window() - get the global window object', () => {
    // https://on.cypress.io/window
    cy.window().should('have.property', 'top')
  })

  it('cy.document() - get the document object', () => {
    // https://on.cypress.io/document
    cy.document().should('have.property', 'charset').and('eq', 'UTF-8')
  })

  it('cy.title() - get the title', () => {
    // https://on.cypress.io/title
    cy.title().should('include', 'Homepage')
  })

  it('upload image with no dress', () => {
    uploadImage('nodress.jpg');

    cy.title().should('include', 'Homepage')

    cy.get('cx-image-holder').should('have.length', 0)

    cy.get('.alert-icon').should('have.length', 1)

    cy.get('.alert-danger span:last').contains('Error while uploading image. Please try again')

  })


  it('upload image with one dress', () => {
    const imageName     = 'YSN60O11_8I5.jpg';
    const numberDresses = 1;
    const productId     = '/,product,H0421C012-E11,Hollis';

    upload(imageName, numberDresses, productId);
    //upload('YSN60O11_8I5.jpg', 1, '/,product,H0421C012-E11,Hollis');
  })

  it('upload image with four dresses', () => {
    const imageName     = 'maxresdefault.jpg';
    const numberDresses = 4;
    const productId     = '/,product,OB121C0HR-O11,Object';

    upload(imageName, numberDresses, productId);
  })

  function uploadImage(imageName) {
    // see https://github.com/cypress-io/cypress/issues/170#issuecomment-591651865
    cy.fixture(imageName).as('logo')
    .get('input[type=file]').then(function(el) {
      return Cypress.Blob.base64StringToBlob(this.logo, 'image/jpeg')
        .then((blob) => {
          const file = new File([blob], 'fileName.jpg', { type: 'image/jpeg' })
          const dt = new DataTransfer();
          dt.items.add(file);
          el[0].files = dt.files;
          el[0].dispatchEvent(new Event('change', {bubbles: true}))
        })
    })
  }

  function upload(imageName, numDresses, productId) {

    uploadImage(imageName);

    cy.title().should('include', 'results for "visual-search"')

    cy.get('.alert-icon').should('have.length', 0)

    cy.get('cx-image-holder').should('have.length', 1)

    cy.get('cx-image-holder a[title="Dresses"]').should('have.length', numDresses)

    cy.get('cx-image-holder a:last').click()

    cy.get('a[ng-reflect-router-link="' + productId + '"]').should('have.length', 2)
  }

})

