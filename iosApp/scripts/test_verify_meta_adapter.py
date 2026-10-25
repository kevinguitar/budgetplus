import unittest

from verify_meta_adapter import ADAPTER_CLASS, class_definitions


def metadata(flags, name=ADAPTER_CLASS):
    return f"""        flags          {flags}
        instanceStart  8
        instanceSize   56
        reserved       0x0
        ivarLayout     0x1040e296e
        layout map     0x06
        name           0x1040e6094 {name}
"""


class ClassDefinitionsTests(unittest.TestCase):
    def test_counts_real_class_but_not_metaclass(self):
        output = "Contents of (__DATA_CONST,__objc_classlist) section\n"
        output += metadata("0x184") + metadata("0x185 RO_META")
        self.assertEqual(class_definitions(output.splitlines())[ADAPTER_CLASS], 1)

    def test_supports_data_section_class_list(self):
        output = "Contents of (__DATA,__objc_classlist) section\n" + metadata("0x184")
        self.assertEqual(class_definitions(output.splitlines())[ADAPTER_CLASS], 1)

    def test_metaclass_alone_is_not_a_definition(self):
        output = "Contents of (__DATA_CONST,__objc_classlist) section\n" + metadata("0x185 RO_META")
        self.assertEqual(class_definitions(output.splitlines())[ADAPTER_CLASS], 0)

    def test_rejects_class_references_and_unrelated_sections(self):
        output = "Contents of (__DATA_CONST,__objc_classlist) section\n"
        output += metadata("0x184", "AnotherAdapter")
        output += "Contents of (__DATA,__objc_classrefs) section\n" + metadata("0x184")
        self.assertEqual(class_definitions(output.splitlines())[ADAPTER_CLASS], 0)

    def test_class_name_string_alone_is_not_proof(self):
        self.assertEqual(class_definitions([ADAPTER_CLASS])[ADAPTER_CLASS], 0)

    def test_duplicate_definitions_are_detected(self):
        output = "Contents of (__DATA_CONST,__objc_classlist) section\n" + metadata("0x184") * 2
        self.assertEqual(class_definitions(output.splitlines())[ADAPTER_CLASS], 2)


if __name__ == "__main__":
    unittest.main()
