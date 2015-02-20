from setuptools import setup

requires = [
    'requests>=2.3.0'
]

setup(
    name='datacat',
    version='0.3-SNAPSHOT',
    packages=['datacat'],
    url='http://srs.slac.stanford.edu',
    license='',
    author='Brian Van Klaveren',
    author_email='bvan@slac.stanford.edu',
    description='Data Catalog RESTful client library',
    zip_safe=False,
    install_requires=requires,
    entry_points={
        'console_scripts': ['datacat = datacat.cli:main'],
    }
)
