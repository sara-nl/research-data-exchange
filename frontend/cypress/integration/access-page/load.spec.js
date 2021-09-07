/// <reference types="cypress" />

describe('RDX access page', () => {
  beforeEach(() => {
    cy.intercept('GET', '**/api/dataset/10.21942%2Fuva.14680362.v3', {
      fixture: '10.21942%2Fuva.14680362.v3.json',
    }).as('getDataset');
    cy.visit('http://localhost:3000/access/10.21942%2Fuva.14680362.v3');
  });

  it('displays two todo items by default', () => {
    cy.get('dataset-title h4').should('have.text', 'Dataset My Dataset');
  });
});
