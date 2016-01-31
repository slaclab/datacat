from setuptools import setup

requires = [
    'requests'
]

setup(
    name='datacat',
    version='0.5',
    packages=['datacat'],
    url='http://srs.slac.stanford.edu',
    license='SLAC BSD',
    author='Brian Van Klaveren',
    author_email='bvan@slac.stanford.edu',
    description='Datacat client library',
    install_requires=requires,
    entry_points={
        'console_scripts': ['datacat = datacat.cli:main'],
    }
)
