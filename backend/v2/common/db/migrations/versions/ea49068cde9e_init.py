"""Init

Revision ID: ea49068cde9e
Revises: 
Create Date: 2022-10-19 12:35:22.760357

"""
import sqlalchemy as sa
import sqlmodel
from alembic import op

# revision identifiers, used by Alembic.
revision = "ea49068cde9e"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    # ### commands auto generated by Alembic - please adjust! ###
    op.create_table(
        "rdx_analyst",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("email", sqlmodel.sql.sqltypes.AutoString(), nullable=False),
        sa.Column("token", sqlmodel.sql.sqltypes.AutoString(), nullable=False),
        sa.Column("token_expires_at", sa.DateTime(), nullable=False),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        op.f("ix_rdx_analyst_email"), "rdx_analyst", ["email"], unique=False
    )
    op.create_index(
        op.f("ix_rdx_analyst_token"), "rdx_analyst", ["token"], unique=False
    )
    op.create_table(
        "rdx_user",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("email", sqlmodel.sql.sqltypes.AutoString(), nullable=False),
        sa.Column("token", sqlmodel.sql.sqltypes.AutoString(), nullable=False),
        sa.Column("token_expires_at", sa.DateTime(), nullable=False),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_rdx_user_email"), "rdx_user", ["email"], unique=False)
    op.create_index(op.f("ix_rdx_user_token"), "rdx_user", ["token"], unique=False)
    op.create_table(
        "rdx_dataset",
        sa.Column("share_id", sa.Integer(), nullable=False),
        sa.Column("doi", sqlmodel.sql.sqltypes.AutoString(), nullable=True),
        sa.Column("title", sqlmodel.sql.sqltypes.AutoString(), nullable=True),
        sa.Column("authors", sqlmodel.sql.sqltypes.AutoString(), nullable=True),
        sa.Column("description", sqlmodel.sql.sqltypes.AutoString(), nullable=True),
        sa.Column("files", sqlmodel.sql.sqltypes.AutoString(), nullable=False),
        sa.Column("conditions_url", sqlmodel.sql.sqltypes.AutoString(), nullable=False),
        sa.Column("condtions_share_id", sa.Integer(), nullable=False),
        sa.Column("published", sa.Boolean(), nullable=False),
        sa.Column("published_at", sa.DateTime(), nullable=True),
        sa.Column("data_steward_id", sa.Integer(), nullable=True),
        sa.Column("researcher_id", sa.Integer(), nullable=True),
        sa.Column("id", sa.Integer(), nullable=False),
        sa.ForeignKeyConstraint(
            ["data_steward_id"],
            ["rdx_user.id"],
        ),
        sa.ForeignKeyConstraint(
            ["researcher_id"],
            ["rdx_user.id"],
        ),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        op.f("ix_rdx_dataset_condtions_share_id"),
        "rdx_dataset",
        ["condtions_share_id"],
        unique=False,
    )
    op.create_index(
        op.f("ix_rdx_dataset_data_steward_id"),
        "rdx_dataset",
        ["data_steward_id"],
        unique=False,
    )
    op.create_index(op.f("ix_rdx_dataset_doi"), "rdx_dataset", ["doi"], unique=False)
    op.create_index(
        op.f("ix_rdx_dataset_researcher_id"),
        "rdx_dataset",
        ["researcher_id"],
        unique=False,
    )
    op.create_index(
        op.f("ix_rdx_dataset_share_id"), "rdx_dataset", ["share_id"], unique=False
    )
    op.create_table(
        "rdx_share",
        sa.Column("share_id", sa.Integer(), nullable=False),
        sa.Column("path", sqlmodel.sql.sqltypes.AutoString(), nullable=False),
        sa.Column("uid_owner", sqlmodel.sql.sqltypes.AutoString(), nullable=True),
        sa.Column(
            "additional_info_owner", sqlmodel.sql.sqltypes.AutoString(), nullable=True
        ),
        sa.Column("permissions", sa.Integer(), nullable=False),
        sa.Column("rdx_dataset_id", sa.Integer(), nullable=True),
        sa.Column("id", sa.Integer(), nullable=False),
        sa.ForeignKeyConstraint(
            ["rdx_dataset_id"],
            ["rdx_dataset.id"],
        ),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        op.f("ix_rdx_share_share_id"), "rdx_share", ["share_id"], unique=False
    )
    # ### end Alembic commands ###


def downgrade() -> None:
    # ### commands auto generated by Alembic - please adjust! ###
    op.drop_index(op.f("ix_rdx_share_share_id"), table_name="rdx_share")
    op.drop_table("rdx_share")
    op.drop_index(op.f("ix_rdx_dataset_share_id"), table_name="rdx_dataset")
    op.drop_index(op.f("ix_rdx_dataset_researcher_id"), table_name="rdx_dataset")
    op.drop_index(op.f("ix_rdx_dataset_doi"), table_name="rdx_dataset")
    op.drop_index(op.f("ix_rdx_dataset_data_steward_id"), table_name="rdx_dataset")
    op.drop_index(op.f("ix_rdx_dataset_condtions_share_id"), table_name="rdx_dataset")
    op.drop_table("rdx_dataset")
    op.drop_index(op.f("ix_rdx_user_token"), table_name="rdx_user")
    op.drop_index(op.f("ix_rdx_user_email"), table_name="rdx_user")
    op.drop_table("rdx_user")
    op.drop_index(op.f("ix_rdx_analyst_token"), table_name="rdx_analyst")
    op.drop_index(op.f("ix_rdx_analyst_email"), table_name="rdx_analyst")
    op.drop_table("rdx_analyst")
    # ### end Alembic commands ###
