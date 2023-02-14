"""Change access license from enum to int

Revision ID: 3c5a0c1a00fe
Revises: a75b01b488f5
Create Date: 2023-02-13 16:31:59.746995

"""
import sqlalchemy as sa
import sqlmodel
from alembic import op
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = "3c5a0c1a00fe"
down_revision = "a75b01b488f5"
branch_labels = None
depends_on = None


def upgrade() -> None:
    # ### commands auto generated by Alembic - please adjust! ###
    op.add_column(
        "rdx_dataset", sa.Column("access_license_id", sa.Integer(), nullable=True)
    )
    op.drop_column("rdx_dataset", "access_license")
    # ### end Alembic commands ###


def downgrade() -> None:
    # ### commands auto generated by Alembic - please adjust! ###
    op.add_column(
        "rdx_dataset",
        sa.Column(
            "access_license",
            postgresql.ENUM("download", "analyze", name="accesslicense"),
            autoincrement=False,
            nullable=True,
        ),
    )
    op.drop_column("rdx_dataset", "access_license_id")
    # ### end Alembic commands ###
