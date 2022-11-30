import { Navbar, Nav } from 'react-bootstrap';
import Link from 'next/link';
import Image from 'next/image';
import { useEffect } from 'react';

type Props = {
  email?: string;
};

const NavBarComponent: React.FC<Props> = (props) => {
  useEffect(() => {
    const button = document.querySelector('.navbar-toggler');
    button.addEventListener('click', () => {
      const children = button.children;
      children[0].classList.toggle('navbar-activated');
    });
  }, []);

  return (
    <header>
      <Navbar bg="light" collapseOnSelect expand="lg">
        <Navbar.Brand><img className="home-logo" src="/images/logos/surf.png" />Research Data Exchange</Navbar.Brand>
        <Navbar.Toggle />
        {props.email && (
          <Navbar.Collapse className="justify-content-end">
            <Nav className="d-flex align-items-center">
              <Image
                src="/images/person-circle.svg"
                width="30px"
                height="30px"
              />
              <Navbar.Text className="ml-2">{props.email}</Navbar.Text>
            </Nav>
          </Navbar.Collapse>
        )}
      </Navbar>
    </header>
  );
};

export default NavBarComponent;
