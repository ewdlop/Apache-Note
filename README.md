# Apache-Note

https://www.youtube.com/watch?v=C4fUGyfuJMw

```sql
-- 1. Creating Tables with Constraints
CREATE TABLE employees (
    employee_id NUMBER PRIMARY KEY,
    first_name VARCHAR2(50),
    last_name VARCHAR2(50) NOT NULL,
    email VARCHAR2(100) UNIQUE,
    hire_date DATE DEFAULT SYSDATE,
    salary NUMBER(10,2) CHECK (salary > 0),
    department_id NUMBER,
    CONSTRAINT fk_department 
        FOREIGN KEY (department_id) 
        REFERENCES departments(department_id)
);

CREATE TABLE departments (
    department_id NUMBER PRIMARY KEY,
    department_name VARCHAR2(100) NOT NULL,
    location_id NUMBER,
    manager_id NUMBER
);

-- 2. Sequences for Auto-incrementing IDs
CREATE SEQUENCE emp_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- 3. Stored Procedure Example
CREATE OR REPLACE PROCEDURE hire_employee(
    p_first_name IN VARCHAR2,
    p_last_name IN VARCHAR2,
    p_email IN VARCHAR2,
    p_salary IN NUMBER,
    p_department_id IN NUMBER,
    p_employee_id OUT NUMBER
) IS
BEGIN
    -- Get next employee ID from sequence
    SELECT emp_seq.NEXTVAL INTO p_employee_id FROM DUAL;
    
    -- Insert new employee
    INSERT INTO employees (
        employee_id,
        first_name,
        last_name,
        email,
        salary,
        department_id
    ) VALUES (
        p_employee_id,
        p_first_name,
        p_last_name,
        p_email,
        p_salary,
        p_department_id
    );
    
    -- Commit the transaction
    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        -- Rollback on error
        ROLLBACK;
        -- Re-raise the exception
        RAISE;
END hire_employee;
/

-- 4. Function Example
CREATE OR REPLACE FUNCTION calculate_bonus(
    p_salary IN NUMBER,
    p_years_service IN NUMBER
) RETURN NUMBER IS
    v_bonus NUMBER;
BEGIN
    v_bonus := CASE
        WHEN p_years_service < 2 THEN p_salary * 0.05
        WHEN p_years_service < 5 THEN p_salary * 0.10
        ELSE p_salary * 0.15
    END;
    
    RETURN v_bonus;
END calculate_bonus;
/

-- 5. Trigger Example
CREATE OR REPLACE TRIGGER employee_audit_trg
AFTER INSERT OR UPDATE OR DELETE ON employees
FOR EACH ROW
BEGIN
    IF INSERTING THEN
        INSERT INTO employee_audit (
            action_type,
            employee_id,
            action_date,
            modified_by
        ) VALUES (
            'INSERT',
            :NEW.employee_id,
            SYSDATE,
            USER
        );
    ELSIF UPDATING THEN
        INSERT INTO employee_audit (
            action_type,
            employee_id,
            action_date,
            modified_by,
            old_salary,
            new_salary
        ) VALUES (
            'UPDATE',
            :OLD.employee_id,
            SYSDATE,
            USER,
            :OLD.salary,
            :NEW.salary
        );
    ELSIF DELETING THEN
        INSERT INTO employee_audit (
            action_type,
            employee_id,
            action_date,
            modified_by
        ) VALUES (
            'DELETE',
            :OLD.employee_id,
            SYSDATE,
            USER
        );
    END IF;
END;
/

-- 6. Complex Query Example with Multiple Joins
SELECT 
    e.employee_id,
    e.first_name || ' ' || e.last_name AS full_name,
    d.department_name,
    m.first_name || ' ' || m.last_name AS manager_name,
    e.salary,
    calculate_bonus(e.salary, 
        FLOOR(MONTHS_BETWEEN(SYSDATE, e.hire_date)/12)
    ) AS bonus,
    COUNT(*) OVER (PARTITION BY e.department_id) AS dept_employee_count,
    AVG(e.salary) OVER (PARTITION BY e.department_id) AS dept_avg_salary,
    RANK() OVER (
        PARTITION BY e.department_id 
        ORDER BY e.salary DESC
    ) AS salary_rank_in_dept
FROM 
    employees e
    LEFT JOIN departments d ON e.department_id = d.department_id
    LEFT JOIN employees m ON d.manager_id = m.employee_id
WHERE 
    e.salary > (
        SELECT AVG(salary) 
        FROM employees
    )
ORDER BY 
    d.department_name,
    e.salary DESC;

-- 7. Materialized View Example
CREATE MATERIALIZED VIEW mv_department_stats
REFRESH ON COMMIT
AS
SELECT 
    d.department_id,
    d.department_name,
    COUNT(e.employee_id) AS employee_count,
    AVG(e.salary) AS avg_salary,
    MAX(e.salary) AS max_salary,
    MIN(e.salary) AS min_salary,
    SUM(e.salary) AS total_salary_cost
FROM 
    departments d
    LEFT JOIN employees e ON d.department_id = e.department_id
GROUP BY 
    d.department_id,
    d.department_name;

-- 8. Index Creation for Performance
CREATE INDEX idx_emp_dept_id ON employees(department_id);
CREATE INDEX idx_emp_name ON employees(last_name, first_name);

-- Bitmap index for low-cardinality columns
CREATE BITMAP INDEX idx_emp_dept_id_bitmap ON employees(department_id);

-- Function-based index
CREATE INDEX idx_emp_email_upper ON employees(UPPER(email));

-- 9. Package Example
CREATE OR REPLACE PACKAGE emp_mgmt AS
    -- Public variable
    g_min_salary CONSTANT NUMBER := 30000;
    
    -- Public procedures and functions
    PROCEDURE hire_employee(
        p_first_name IN VARCHAR2,
        p_last_name IN VARCHAR2,
        p_email IN VARCHAR2,
        p_salary IN NUMBER,
        p_department_id IN NUMBER,
        p_employee_id OUT NUMBER
    );
    
    FUNCTION calculate_bonus(
        p_salary IN NUMBER,
        p_years_service IN NUMBER
    ) RETURN NUMBER;
    
    FUNCTION get_employee_count(
        p_department_id IN NUMBER
    ) RETURN NUMBER;
END emp_mgmt;
/

CREATE OR REPLACE PACKAGE BODY emp_mgmt AS
    -- Private variable
    v_last_error VARCHAR2(1000);
    
    -- Procedure implementation
    PROCEDURE hire_employee(
        p_first_name IN VARCHAR2,
        p_last_name IN VARCHAR2,
        p_email IN VARCHAR2,
        p_salary IN NUMBER,
        p_department_id IN NUMBER,
        p_employee_id OUT NUMBER
    ) IS
    BEGIN
        IF p_salary < g_min_salary THEN
            RAISE_APPLICATION_ERROR(-20001, 
                'Salary cannot be less than minimum salary');
        END IF;
        
        SELECT emp_seq.NEXTVAL INTO p_employee_id FROM DUAL;
        
        INSERT INTO employees (
            employee_id,
            first_name,
            last_name,
            email,
            salary,
            department_id
        ) VALUES (
            p_employee_id,
            p_first_name,
            p_last_name,
            p_email,
            p_salary,
            p_department_id
        );
        
        COMMIT;
    EXCEPTION
        WHEN OTHERS THEN
            v_last_error := SQLERRM;
            ROLLBACK;
            RAISE;
    END hire_employee;
    
    -- Function implementation
    FUNCTION calculate_bonus(
        p_salary IN NUMBER,
        p_years_service IN NUMBER
    ) RETURN NUMBER IS
        v_bonus NUMBER;
    BEGIN
        v_bonus := CASE
            WHEN p_years_service < 2 THEN p_salary * 0.05
            WHEN p_years_service < 5 THEN p_salary * 0.10
            ELSE p_salary * 0.15
        END;
        
        RETURN v_bonus;
    END calculate_bonus;
    
    -- Function implementation
    FUNCTION get_employee_count(
        p_department_id IN NUMBER
    ) RETURN NUMBER IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*)
        INTO v_count
        FROM employees
        WHERE department_id = p_department_id;
        
        RETURN v_count;
    END get_employee_count;
END emp_mgmt;
/

-- 10. Example Usage
DECLARE
    v_employee_id NUMBER;
BEGIN
    -- Hire new employee
    emp_mgmt.hire_employee(
        p_first_name => 'John',
        p_last_name => 'Doe',
        p_email => 'john.doe@example.com',
        p_salary => 50000,
        p_department_id => 1,
        p_employee_id => v_employee_id
    );
    
    -- Calculate bonus
    DBMS_OUTPUT.PUT_LINE(
        'Bonus: ' || 
        emp_mgmt.calculate_bonus(50000, 3)
    );
    
    -- Get department employee count
    DBMS_OUTPUT.PUT_LINE(
        'Department employees: ' || 
        emp_mgmt.get_employee_count(1)
    );
END;
/
```
