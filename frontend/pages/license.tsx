import React from 'react';
import Footer from '../components/footer';
import Head from 'next/head';
import { Carousel, Col, Container, Row } from 'react-bootstrap';

const Home: React.FC = () => (
  <main>
    <Head>
      <link
        rel="shortcut icon"
        href="/images/rdx-logo.png"
        type="image/x-icon"
      />
      <title>Research Data Exchange MVP</title>
    </Head>
    <section className="mt-5">
      <Container>
        <Row className="mt-5">
          <Col>
            <h2 className="display-4 text-center">
              Research Data Exchange License
            </h2>
            <p className="lead mt-5">
              Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec
              rutrum augue sed hendrerit elementum. Class aptent taciti sociosqu
              ad litora torquent per conubia nostra, per inceptos himenaeos.
              Nullam ac porttitor urna. In venenatis nisi at nisi imperdiet, et
              finibus sapien pretium. Morbi sollicitudin semper eros, sit amet
              finibus velit congue dapibus. Etiam ullamcorper elit a massa
              mollis, vitae rhoncus orci finibus. Vivamus quis viverra mauris.
              Maecenas ac gravida ipsum. Donec ornare lacus eu nulla finibus, id
              cursus ex dictum. Aliquam fermentum vehicula ligula. Donec nec
              eros in lorem laoreet porttitor sed vitae neque. Nunc aliquet erat
              arcu, in elementum justo hendrerit in.
            </p>
            <p className="lead mt-5">
              Sed ac congue augue. In nec dolor vitae dolor pulvinar tincidunt
              vel non sem. Curabitur commodo sodales orci eget eleifend. Cras
              sollicitudin accumsan nisi, vel blandit purus lobortis ut. Morbi
              interdum dolor dolor, ac lobortis neque mattis et. Nullam eu
              tortor aliquet turpis sodales viverra eu eu ligula. Etiam
              convallis massa ut lacus ultricies, vel finibus mauris feugiat.
              Proin a ligula vulputate, faucibus urna sit amet, pretium sem.
              Suspendisse vel enim mauris. Cras suscipit libero metus, vitae
              dapibus lacus venenatis et. Aliquam placerat molestie mauris,
              vitae consectetur sapien eleifend sit amet.
            </p>
            <p className="lead mt-5">
              Maecenas rhoncus, ante a luctus malesuada, felis nisl vehicula
              dolor, non consequat tellus tellus eget lacus. Phasellus sit amet
              interdum libero. Vestibulum libero tellus, lobortis vel erat nec,
              maximus tincidunt sapien. Orci varius natoque penatibus et magnis
              dis parturient montes, nascetur ridiculus mus. Nam magna massa,
              tempus sed iaculis sed, volutpat in lacus. Nullam sit amet gravida
              mi. Nulla ante mi, vulputate vel felis vitae, ullamcorper gravida
              orci. Nullam vel maximus ex. Praesent quis cursus lacus.
              Vestibulum ante ipsum primis in faucibus orci luctus et ultrices
              posuere cubilia curae; Sed a eros tristique, porta lectus quis,
              congue purus. Ut mollis lacus et accumsan congue.
            </p>
            <p className="lead mt-5">
              Cras posuere tempus commodo. Suspendisse porta, nisl et interdum
              congue, eros erat tempor felis, rhoncus fermentum libero ipsum vel
              massa. Maecenas accumsan ipsum ut massa egestas, quis fermentum
              sapien suscipit. Ut varius nisl sed tellus ornare, vitae molestie
              justo malesuada. Quisque fringilla auctor diam, eget maximus arcu
              commodo eu. Etiam vel enim ligula. Interdum et malesuada fames ac
              ante ipsum primis in faucibus.
            </p>
            <p className="lead mt-5">
              Fusce tempus urna ut porta accumsan. Aliquam commodo mauris ac
              euismod feugiat. Mauris a scelerisque sapien, id tincidunt augue.
              Sed ut posuere quam. Quisque lobortis lacinia consequat. Phasellus
              consequat, neque eu rhoncus varius, lacus odio elementum nibh, ac
              vehicula lectus nulla sed velit. Sed in iaculis lorem. Maecenas
              pulvinar metus ac auctor consectetur. Aliquam rhoncus, quam at
              condimentum scelerisque, metus sem gravida ligula, sit amet
              aliquam elit massa vitae massa.
            </p>
          </Col>
        </Row>
      </Container>
    </section>
    <Footer />
  </main>
);

export default Home;
