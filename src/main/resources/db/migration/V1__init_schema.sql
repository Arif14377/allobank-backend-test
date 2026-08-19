CREATE TABLE users (
    id UUID PRIMARY KEY,
    full_name VARCHAR NOT NULL,
    email VARCHAR UNIQUE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE bill_groups (
    id UUID PRIMARY KEY,
    name VARCHAR NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE bill_group_members (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES bill_groups(id),
    user_id UUID NOT NULL REFERENCES users(id),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uk_bill_group_members_group_user UNIQUE (group_id, user_id)
);

CREATE TABLE bills (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES bill_groups(id),
    payer_id UUID NOT NULL REFERENCES users(id),
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    description VARCHAR,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE bill_debtors (
    id UUID PRIMARY KEY,
    bill_id UUID NOT NULL REFERENCES bills(id),
    debtor_id UUID NOT NULL REFERENCES users(id),
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_bill_debtors_bill_debtor UNIQUE (bill_id, debtor_id)
);

CREATE INDEX idx_bill_groups_created_by ON bill_groups(created_by);
CREATE INDEX idx_bill_group_members_user_id ON bill_group_members(user_id);
CREATE INDEX idx_bills_group_id ON bills(group_id);
CREATE INDEX idx_bills_payer_id ON bills(payer_id);
CREATE INDEX idx_bill_debtors_bill_id ON bill_debtors(bill_id);
CREATE INDEX idx_bill_debtors_debtor_id ON bill_debtors(debtor_id);
