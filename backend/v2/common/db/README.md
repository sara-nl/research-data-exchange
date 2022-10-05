# DB

The db library implements a postgres database client that can be used in other components.
It uses the credentials and settings specified in the `.env` file.

## Packages used

* [SQLModel](https://sqlmodel.tiangolo.com/) creates a Pydantic wrapper around SQLAlchemy, so the models are typed. It also integrates nicely with FastAPI.
* [SQLAlchemy](https://docs.sqlalchemy.org/en/14/index.html) the underlying ORM used by SQLModel.
* [Alembic](https://alembic.sqlalchemy.org/en/latest/) for creating and applying database migrations.

## Create and apply migrations

First, make sure all model files are exported with the `__all__` variable in the [`__init__.py` file](../models/__init__.py).
Alembic uses the metadata in SQLModel to generate migrations. In turn, SQLModel uses the actual application models to generate the metadata.

Create migration from the `backend/v2` directory:

```{bash}
alembic revision --autogenerate -m "<message>"
```

Apply migrations:

```{bash}
alembic upgrade head
```

## Example

```{python}
from typing import Optional

from sqlmodel import Field, SQLModel

from common.db.db_client import DBClient


# Example model. Would normally be defined in the common/models directory
class Hero(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    name: str
    secret_name: str
    age: Optional[int] = None


# Create database client, you only need one per application
db = DBClient()

# This would normally be done with Alembic migrations
SQLModel.metadata.create_all(db.engine)

batman = Hero(name="batman", secret_name="secret batman", age=42)

with db.getSession() as session:
    session.add(batman)
    session.commit()
```
