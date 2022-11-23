/// <reference types="cypress" />
describe('RDX access page', () => {
  beforeEach(() => {
    cy.intercept('GET', '**/api/dataset/10.21942/uva.14680362.v3/access', {
      fixture: '10.21942%2Fuva.14680362.v3.json',
    }).as('getDataset');

    //todo do as env var
    cy.task('mockServer', {
      interceptUrl: `${Cypress.env(
        'RDX_BACKEND_URL',
      )}/api/dataset/10.21942%2Fuva.14680362.v3/access`,
      fixture: '10.21942%2Fuva.14680362.v3.json',
    });

    cy.visit('http://localhost:3000/access/10.21942/uva.14680362.v3');
  });

  it('displays dataset details returned by the service', () => {
    cy.get('.dataset-title h4').should('have.text', 'My Dataset');
    cy.get('.dataset-title p').should('have.text', 'Foo');
    cy.get('.dataset-content span.dataset-file')
      .first()
      .should('have.text', 'conditions.pdf');
    cy.get('.dataset-content span.dataset-file').last().should('have.text', '1.csv');
  });

  //todo pdf file is loaded into the viewer
  // accept conditions checkbox is disabled when page loads
});

export {}
