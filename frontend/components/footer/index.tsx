import { Container } from 'react-bootstrap';

const Footer: React.FC = () => (
  <footer className="footer px-2 px-md-5 py-4 mt-5">
    <Container fluid={true}>
      <div className="text-center">
        <p>
          This is a prototype of SURF Research Data Exchange. Send an email to{' '}
          <a href="mailto:support@rdx.labs.surf.nl">support@rdx.labs.surf.nl</a>{' '}
          in case you need any assistance.
        </p>
      </div>
    </Container>
  </footer>
);

export default Footer;
