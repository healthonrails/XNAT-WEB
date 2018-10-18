INSERT INTO xdat_element_access (element_name, xdat_usergroup_xdat_usergroup_id)
SELECT :elementName AS element_name, xdat_userGroup_id
FROM
     xdat_userGroup ug
       LEFT JOIN xdat_element_access ea ON ug.xdat_usergroup_id = ea.xdat_usergroup_xdat_usergroup_id AND ea.element_name = :elementName
WHERE
  ea.element_name IS NULL AND
  ug.tag IS NOT NULL;

INSERT INTO xdat_field_mapping_set (method, permissions_allow_set_xdat_elem_xdat_element_access_id)
SELECT 'OR' AS method, xdat_element_access_id
FROM
     xdat_element_access ea
       LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id = fms.permissions_allow_set_xdat_elem_xdat_element_access_id
WHERE
  ea.element_name = :elementName AND
  fms.method IS NULL;

INSERT INTO xdat_field_mapping (field, field_value, create_element, read_element, edit_element, delete_element, active_element, comparison_type, xdat_field_mapping_set_xdat_field_mapping_set_id)
SELECT :elementName || '/project' AS field, ug.tag, 1, 1, 1, 1, 1, 'equals', fms.xdat_field_mapping_set_id
FROM
     xdat_userGroup ug
       LEFT JOIN xdat_element_access ea ON ug.xdat_usergroup_id = ea.xdat_usergroup_xdat_usergroup_id AND ea.element_name = :elementName
       LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id = fms.permissions_allow_set_xdat_elem_xdat_element_access_id
       LEFT JOIN xdat_field_mapping fm ON fms.xdat_field_mapping_set_id = fm.xdat_field_mapping_set_xdat_field_mapping_set_id
WHERE
  ug.displayname = 'Owners' AND
  fm.field IS NULL AND
  ug.tag IS NOT NULL
UNION
SELECT :elementName || '/sharing/share/project' AS field, ug.tag, 0, 1, 0, 0, 1, 'equals', fms.xdat_field_mapping_set_id
FROM
     xdat_userGroup ug
       LEFT JOIN xdat_element_access ea ON ug.xdat_usergroup_id = ea.xdat_usergroup_xdat_usergroup_id AND ea.element_name = :elementName
       LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id = fms.permissions_allow_set_xdat_elem_xdat_element_access_id
       LEFT JOIN xdat_field_mapping fm ON fms.xdat_field_mapping_set_id = fm.xdat_field_mapping_set_xdat_field_mapping_set_id
WHERE
  ug.displayname = 'Owners' AND
  fm.field IS NULL AND
  ug.tag IS NOT NULL
UNION
SELECT :elementName || '/project' AS field, ug.tag, 1, 1, 1, 0, 0, 'equals', fms.xdat_field_mapping_set_id
FROM
     xdat_userGroup ug
       LEFT JOIN xdat_element_access ea ON ug.xdat_usergroup_id = ea.xdat_usergroup_xdat_usergroup_id AND ea.element_name = :elementName
       LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id = fms.permissions_allow_set_xdat_elem_xdat_element_access_id
       LEFT JOIN xdat_field_mapping fm ON fms.xdat_field_mapping_set_id = fm.xdat_field_mapping_set_xdat_field_mapping_set_id
WHERE
  ug.displayname = 'Members' AND
  fm.field IS NULL AND
  ug.tag IS NOT NULL
UNION
SELECT :elementName || '/sharing/share/project' AS field, ug.tag, 0, 1, 0, 0, 0, 'equals', fms.xdat_field_mapping_set_id
FROM
     xdat_userGroup ug
       LEFT JOIN xdat_element_access ea ON ug.xdat_usergroup_id = ea.xdat_usergroup_xdat_usergroup_id AND ea.element_name = :elementName
       LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id = fms.permissions_allow_set_xdat_elem_xdat_element_access_id
       LEFT JOIN xdat_field_mapping fm ON fms.xdat_field_mapping_set_id = fm.xdat_field_mapping_set_xdat_field_mapping_set_id
WHERE
  ug.displayname = 'Members' AND
  fm.field IS NULL AND
  ug.tag IS NOT NULL
UNION
SELECT :elementName || '/project' AS field, ug.tag, 0, 1, 0, 0, 0, 'equals', fms.xdat_field_mapping_set_id
FROM
     xdat_userGroup ug
       LEFT JOIN xdat_element_access ea ON ug.xdat_usergroup_id = ea.xdat_usergroup_xdat_usergroup_id AND ea.element_name = :elementName
       LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id = fms.permissions_allow_set_xdat_elem_xdat_element_access_id
       LEFT JOIN xdat_field_mapping fm ON fms.xdat_field_mapping_set_id = fm.xdat_field_mapping_set_xdat_field_mapping_set_id
WHERE
  ug.displayname = 'Collaborators' AND
  fm.field IS NULL AND
  ug.tag IS NOT NULL
UNION
SELECT :elementName || '/sharing/share/project' AS field, ug.tag, 0, 1, 0, 0, 0, 'equals', fms.xdat_field_mapping_set_id
FROM
     xdat_userGroup ug
       LEFT JOIN xdat_element_access ea ON ug.xdat_usergroup_id = ea.xdat_usergroup_xdat_usergroup_id AND ea.element_name = :elementName
       LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id = fms.permissions_allow_set_xdat_elem_xdat_element_access_id
       LEFT JOIN xdat_field_mapping fm ON fms.xdat_field_mapping_set_id = fm.xdat_field_mapping_set_xdat_field_mapping_set_id
WHERE
  ug.displayname = 'Collaborators' AND
  fm.field IS NULL AND
  ug.tag IS NOT NULL;

INSERT INTO xdat_element_access (element_name, xdat_usergroup_xdat_usergroup_id)
SELECT :elementName AS element_name, xdat_userGroup_id
FROM
     xdat_userGroup ug
       LEFT JOIN xdat_element_access ea ON ug.xdat_usergroup_id = ea.xdat_usergroup_xdat_usergroup_id AND ea.element_name = :elementName
WHERE
  ea.element_name IS NULL AND
  (id = 'ALL_DATA_ADMIN' OR id = 'ALL_DATA_ACCESS');

INSERT INTO xdat_field_mapping_set (method, permissions_allow_set_xdat_elem_xdat_element_access_id)
SELECT 'OR' AS method, xdat_element_access_id
FROM
     xdat_element_access ea
       LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id = fms.permissions_allow_set_xdat_elem_xdat_element_access_id
WHERE
  ea.element_name = :elementName AND
  fms.method IS NULL;

INSERT INTO xdat_field_mapping (field, field_value, create_element, read_element, edit_element, delete_element, active_element, comparison_type, xdat_field_mapping_set_xdat_field_mapping_set_id)
SELECT :elementName || '/project' AS field, '*', 1, 1, 1, 1, 1, 'equals', fms.xdat_field_mapping_set_id
FROM
     xdat_userGroup ug
       LEFT JOIN xdat_element_access ea ON ug.xdat_usergroup_id = ea.xdat_usergroup_xdat_usergroup_id AND ea.element_name = :elementName
       LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id = fms.permissions_allow_set_xdat_elem_xdat_element_access_id
       LEFT JOIN xdat_field_mapping fm ON fms.xdat_field_mapping_set_id = fm.xdat_field_mapping_set_xdat_field_mapping_set_id
WHERE
  id = 'ALL_DATA_ADMIN' AND
  fm.field IS NULL
UNION
SELECT :elementName || '/sharing/share/project' AS field, '*', 1, 1, 1, 1, 1, 'equals', fms.xdat_field_mapping_set_id
FROM
     xdat_userGroup ug
       LEFT JOIN xdat_element_access ea ON ug.xdat_usergroup_id = ea.xdat_usergroup_xdat_usergroup_id AND ea.element_name = :elementName
       LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id = fms.permissions_allow_set_xdat_elem_xdat_element_access_id
       LEFT JOIN xdat_field_mapping fm ON fms.xdat_field_mapping_set_id = fm.xdat_field_mapping_set_xdat_field_mapping_set_id
WHERE
  id = 'ALL_DATA_ADMIN' AND
  fm.field IS NULL
UNION
SELECT :elementName || '/project' AS field, '*', 0, 1, 0, 0, 1, 'equals', fms.xdat_field_mapping_set_id
FROM
     xdat_userGroup ug
       LEFT JOIN xdat_element_access ea ON ug.xdat_usergroup_id = ea.xdat_usergroup_xdat_usergroup_id AND ea.element_name = :elementName
       LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id = fms.permissions_allow_set_xdat_elem_xdat_element_access_id
       LEFT JOIN xdat_field_mapping fm ON fms.xdat_field_mapping_set_id = fm.xdat_field_mapping_set_xdat_field_mapping_set_id
WHERE
  id = 'ALL_DATA_ACCESS' AND
  fm.field IS NULL
UNION
SELECT :elementName || '/sharing/share/project' AS field, '*', 0, 1, 0, 0, 1, 'equals', fms.xdat_field_mapping_set_id
FROM
     xdat_userGroup ug
       LEFT JOIN xdat_element_access ea ON ug.xdat_usergroup_id = ea.xdat_usergroup_xdat_usergroup_id AND ea.element_name = :elementName
       LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id = fms.permissions_allow_set_xdat_elem_xdat_element_access_id
       LEFT JOIN xdat_field_mapping fm ON fms.xdat_field_mapping_set_id = fm.xdat_field_mapping_set_xdat_field_mapping_set_id
WHERE
  id = 'ALL_DATA_ACCESS' AND
  fm.field IS NULL;