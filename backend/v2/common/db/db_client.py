import typing

import sqlalchemy
from pydantic import BaseModel, validator
from sqlmodel import Session, create_engine

from .settings import db_settings


class DBClient(BaseModel):
    engine: typing.Any

    @classmethod
    @validator("engine")
    def engine_must_be_an_sqlalchemy_engine_instance(cls, val):
        if not isinstance(val, sqlalchemy.future.Engine):
            raise ValueError("engine must be of type sqlalchemy.future.Engine")
        return val

    def __init__(self, **kwargs):
        conn_string = (
            f"postgresql+psycopg2://{db_settings.user}:{db_settings.password}"
            f"@{db_settings.host}:{db_settings.port}/{db_settings.name}"
        )
        engine = create_engine(conn_string)
        super().__init__(engine=engine, **kwargs)

    def get_session(self) -> Session:
        return Session(self.engine)

    def get_session_dependency(self) -> Session:
        with self.get_session() as session:
            yield session
